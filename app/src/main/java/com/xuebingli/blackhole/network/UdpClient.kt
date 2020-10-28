package com.xuebingli.blackhole.network

import android.util.Log
import com.google.gson.Gson
import com.xuebingli.blackhole.models.PacketReport
import com.xuebingli.blackhole.restful.PourRequest
import com.xuebingli.blackhole.utils.Constants.Companion.M
import com.xuebingli.blackhole.utils.TimeUtils
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.core.ObservableOnSubscribe
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.net.*
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

class UdpClient(
    private val id: String,
    private val ip: String,
    private val port: Int,
    private val bitrate: Int,
    private val packetSize: Int,
    private val duration: Int
) {
    companion object {
        init {
            System.loadLibrary("network-lib")
        }
    }

    private val idBytes = 36

    private external fun udpPourRead(
        ip: String,
        port: Int,
        request: String,
        listener: DatagramListener
    )

    private val udpPourServiceNDK = Observable.create(ObservableOnSubscribe<PacketReport> {
        Log.d(
            "johnson", "Starting UDP pour, ip: $ip, port: $port, bitrate: $bitrate bps, " +
                    "duration: $duration s, id: $id"
        )
        val buf = Gson().toJson(PourRequest(id, "start", packetSize, bitrate, duration))
        udpPourRead(ip, port, buf, DatagramListener(it))
    })

    @ExperimentalUnsignedTypes
    private val udpPourService = Observable.create(ObservableOnSubscribe<PacketReport> {
        Log.d(
            "johnson", "Starting UDP pour, ip: $ip, port: $port, bitrate: $bitrate bps, " +
                    "duration: $duration s, id: $id"
        )
//        val socket = DatagramSocket()
//        socket.soTimeout = 1000
        val address = InetAddress.getByName(ip)
        val buf = ByteBuffer.wrap(
            Gson().toJson(PourRequest(id, "start", packetSize, bitrate, duration)).toByteArray()
        )
        val channel = DatagramChannel.open()
        channel.setOption(StandardSocketOptions.SO_RCVBUF, 4 * M)
        channel.connect(InetSocketAddress(address, port))
        channel.write(buf)
        val buffer = ByteBuffer.allocateDirect(100 * 1024)
        var nread = -1
        while (!it.isDisposed) {
            buffer.clear()
            try {
                nread = channel.read(buffer)
            } catch (e: SocketTimeoutException) {
                Log.w("johnson", e.message, e)
                continue
            }
            if (buffer.position() == 1 && buffer[0] == 'T'.toByte()) {
                break
            }
            var sequence = 0
            for (i in 0..3) {
                sequence = (sequence shl 8) + buffer[i].toUByte().toInt()
            }
            var remoteTimestamp = 0L
            for (i in 4..11) {
                remoteTimestamp = (remoteTimestamp shl 8) + buffer[i].toUByte().toInt()
            }
            it.onNext(
                PacketReport(
                    sequence, buffer.position(),
                    TimeUtils().elapsedRealTime(), remoteTimestamp
                )
            )
        }
        it.onComplete()
    })

    @ExperimentalStdlibApi
    private val udpSinkService = Observable.create(ObservableOnSubscribe<PacketReport> {
        Log.d(
            "johnson", "Starting UDP sink, bitrate: $bitrate bps, " +
                    "packet_size: $packetSize bytes, duration: $duration s, id: $id"
        )
        val buf = ByteArray(packetSize)
        var counter = 0
        id.encodeToByteArray().copyInto(buf, 0, 0, idBytes)
        val socket = DatagramSocket()
        val address = InetAddress.getByName(ip)
        val startTs = TimeUtils().elapsedRealTime()
        while (!it.isDisposed && TimeUtils().elapsedRealTime() - startTs < duration * 1000) {
            val wait = buf.size.toLong() * 8 * counter * 1000 / bitrate -
                    (TimeUtils().elapsedRealTime() - startTs)
            if (wait > 0) {
                try {
                    Thread.sleep(wait)
                } catch (e: InterruptedException) {
                    break
                }
            }
            buf[idBytes] = (counter shr 24).toByte()
            buf[idBytes + 1] = (counter shr 16).toByte()
            buf[idBytes + 2] = (counter shr 8).toByte()
            buf[idBytes + 3] = (counter shr 0).toByte()
            socket.send(DatagramPacket(buf, buf.size, address, port))
            it.onNext(PacketReport(counter, buf.size, TimeUtils().elapsedRealTime()))
            counter++
        }
        socket.close()
        it.onComplete()
    })

    /**
     * callback signature: packet_report, is_last, has_error
     */
    @ExperimentalStdlibApi
    fun startUdpSink(callback: (PacketReport?, Boolean, Boolean) -> Unit): Disposable {
        return udpSinkService.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                callback(it, false, false)
            }, {
                Log.w("johnson", it.message, it)
                callback(null, false, true)
            }, {
                callback(null, true, false)
            })
    }

    @ExperimentalUnsignedTypes
    fun startUdpPour(callback: (PacketReport?, Boolean, Boolean) -> Unit): Disposable {
        return udpPourService.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                callback(it, false, false)
            }, {
                Log.w("johnson", it.message, it)
                callback(null, false, true)
            }, {
                callback(null, true, false)
            })
    }
}

class DatagramListener(private val emitter: ObservableEmitter<PacketReport>) {
    fun onReceived(seq: Int, size: Int, remoteTs: Long, localTs: Long): Boolean {
        if (emitter.isDisposed) {
            return false
        }
        if (size == 1) {
            emitter.onComplete()
            return false
        }
        emitter.onNext(
            PacketReport(
                sequence = seq,
                size = size,
                remoteTimestamp = remoteTs,
                localTimestamp = localTs
            )
        )
        return true
    }
}

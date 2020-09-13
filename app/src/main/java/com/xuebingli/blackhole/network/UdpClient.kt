package com.xuebingli.blackhole.network

import android.os.SystemClock
import android.util.Log
import com.google.gson.Gson
import com.xuebingli.blackhole.restful.PourRequest
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableOnSubscribe
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UdpClient(
    private val id: String,
    private val ip: String,
    private val port: Int,
    private val bitrate: Int,
    private val packetSize: Int,
    private val duration: Int
) {
    private val idBytes = 36

    @ExperimentalUnsignedTypes
    private val udpPourService = Observable.create(ObservableOnSubscribe<PacketReport> {
        Log.d(
            "johnson", "Starting UDP pour, bitrate: $bitrate bps, " +
                    "duration: $duration s, id: $id"
        )
        val buffer = ByteArray(1500)
        val socket = DatagramSocket()
        val address = InetAddress.getByName(ip)
        val buf = Gson().toJson(PourRequest(id, "start", packetSize, bitrate, duration))
            .toByteArray()
        socket.send(DatagramPacket(buf, buf.size, address, port))
        val packet = DatagramPacket(buffer, buffer.size)
        while (!it.isDisposed) {
            socket.receive(packet)
            if (packet.length == 1 && packet.data[0] == 'T'.toByte()) {
                break
            }
            var sequence = 0
            for (i in 0..3) {
                sequence = (sequence shl 8) + packet.data[i].toUByte().toInt()
            }
            var remoteTimestamp = 0L
            for (i in 4..11) {
                remoteTimestamp = (remoteTimestamp shl 8) + packet.data[i].toUByte().toInt()
            }
            it.onNext(
                PacketReport(
                    sequence, packet.length,
                    SystemClock.elapsedRealtime(), remoteTimestamp
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
        val startTs = SystemClock.elapsedRealtime()
        while (!it.isDisposed && SystemClock.elapsedRealtime() - startTs < duration * 1000) {
            val wait = buf.size.toLong() * 8 * counter * 1000 / bitrate -
                    (SystemClock.elapsedRealtime() - startTs)
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
            it.onNext(PacketReport(counter, buf.size, SystemClock.elapsedRealtime(), -1))
            counter++
        }
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

data class PacketReport(
    val sequence: Int,
    val size: Int,
    val localTimestamp: Long,
    val remoteTimestamp: Long
)

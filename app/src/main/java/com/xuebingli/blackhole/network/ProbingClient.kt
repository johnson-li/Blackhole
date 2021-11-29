package com.xuebingli.blackhole.network

import android.util.Log
import com.xuebingli.blackhole.restful.ProbingRecord
import com.xuebingli.blackhole.restful.ProbingRecordType
import com.xuebingli.blackhole.utils.ArrayUtils
import com.xuebingli.blackhole.utils.TimeUtils
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.net.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

class ProbingClient(
    private val serverIP: String,
    private val port: Int,
    private val id: String,
    private val delay: Int,
    private val callback: (ProbingRecord) -> Unit
) {
    companion object {
        const val ID_LENGTH = 36
        const val PACKET_SEQUENCE_BYTES = 4
        const val PACKET_SIZE = ID_LENGTH + PACKET_SEQUENCE_BYTES
    }

    private val disposables = CompositeDisposable()

    @ExperimentalUnsignedTypes
    fun start() {
        val address = InetAddress.getByName(serverIP)
        val socket = DatagramSocket()
        val buffer = ByteArray(PACKET_SIZE).apply {
            id.toByteArray().copyInto(this, 0, 0, ID_LENGTH)
        }
        Observable.create<ProbingRecord> { emitter ->
            socket.connect(address, port)
            Observable.interval(delay.toLong(), TimeUnit.MILLISECONDS).timeInterval()
                .flatMap {
                    ByteBuffer.allocate(PACKET_SEQUENCE_BYTES).apply { order(ByteOrder.BIG_ENDIAN) }
                        .putInt(it.value().toInt()).array()
                        .copyInto(buffer, ID_LENGTH, 0, PACKET_SEQUENCE_BYTES)
                    Observable.create<ProbingRecord> { emitter ->
                        emitter.onNext(
                            ProbingRecord(
                                TimeUtils().getTimeStampAccurate(),
                                it.value().toInt(),
                                ProbingRecordType.SENT
                            )
                        )
                        socket.send(DatagramPacket(buffer, buffer.size))
                    }
                }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe {
                    callback(it)
                }.also { disposables.add(it) }
            val buf = ByteArray(PACKET_SIZE)
            val packet = DatagramPacket(buf, buf.size)
            while (!emitter.isDisposed) {
                socket.receive(packet)
                val sequence = ArrayUtils.bytes2int(buf, ID_LENGTH)
                emitter.onNext(
                    ProbingRecord(
                        TimeUtils().getTimeStampAccurate(),
                        sequence,
                        ProbingRecordType.RECEIVED
                    )
                )
            }
        }.observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe {
            callback(it)
        }.also { disposables.add(it) }
    }

    fun stop() {
        disposables.dispose()
    }
}
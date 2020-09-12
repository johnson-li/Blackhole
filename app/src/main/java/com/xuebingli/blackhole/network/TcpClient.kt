package com.xuebingli.blackhole.network

import android.os.SystemClock
import android.util.Log
import com.google.common.primitives.Longs
import com.google.gson.Gson
import com.xuebingli.blackhole.utils.Constants.Companion.LOG_TIME_FORMAT
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableOnSubscribe
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.net.Socket
import java.util.*
import kotlin.collections.ArrayList

class TcpClient(
    private val ip: String,
    private val port: Int,
    private val dataDir: File
) {
    private val tcpSyncService = Observable.create(ObservableOnSubscribe<SyncResult?> {
        val count = 20
        val buffer = ByteArray(8)
        val localTs = ArrayList<Long>(count)
        val remoteTs = ArrayList<Long>(count)
        val socket = Socket(ip, port).apply { tcpNoDelay = true }
        val output = socket.getOutputStream()
        val input = socket.getInputStream()
        for (i in 1..count) {
            SystemClock.elapsedRealtime().also { ts ->
                Log.d("johnson", "Local ts: $ts")
                localTs.add(ts)
                output.write(Longs.toByteArray(ts).apply { reverse() })
            }
            output.flush()
            val read = input.read(buffer)
            remoteTs.add(Longs.fromByteArray(buffer.apply { reverse() }))
            Log.d("johnson", "Read $read bytes, value: ${remoteTs.last()}")
        }
        val path = File(dataDir, "sync_${LOG_TIME_FORMAT.format(Date())}.json")
        var drift = 0L
        var confidence = 0L
        var minVariance = Long.MAX_VALUE
        for (i in 1 until count) {
            val conf = localTs[i] - localTs[i - 1]
            if (conf < minVariance) {
                minVariance = conf
                confidence = conf / 2
                drift = (localTs[i] + localTs[i - 1]) / 2 - remoteTs[i - 1]
            }
        }
        val result = SyncResult(localTs, remoteTs, drift, confidence)
        path.writeText(Gson().toJson(result))
        it.onNext(result)
        it.onComplete()
    })

    fun startTcpSync(callback: (SyncResult?) -> Unit): Disposable {
        return tcpSyncService.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                callback(it)
            }, {
                Log.w("johnson", it.message, it)
                callback(null)
            })
    }
}

class SyncResult(
    val localTs: List<Long>,
    val remoteTs: List<Long>,
    val clockDrift: Long,
    val confidence: Long
)
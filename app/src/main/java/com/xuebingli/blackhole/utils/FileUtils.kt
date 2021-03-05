package com.xuebingli.blackhole.utils

import android.util.Log
import com.google.gson.Gson
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class FileUtils {
    companion object {
        val FILE_EXECUTOR = ThreadPoolExecutor(2, 4, 4, TimeUnit.SECONDS, LinkedBlockingDeque())
    }

    fun <T> dumpJson(obj: T, file: File, callback: (Boolean) -> Unit) {
        Observable.create<Unit> {
            Gson().toJson(obj).also { str ->
                file.writeText(str)
            }
//            Json.encodeToString(obj).also { str ->
//                file.writeText(str)
//            }
            it.onNext(Unit)
        }.subscribeOn(Schedulers.from(FILE_EXECUTOR)).observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                callback(true)
            }, {
                callback(false)
                Log.e("johnson", it.message, it)
            })
    }
}
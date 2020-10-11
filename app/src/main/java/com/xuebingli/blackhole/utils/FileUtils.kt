package com.xuebingli.blackhole.utils

import android.util.Log
import com.google.gson.Gson
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.io.FileWriter

class FileUtils {
    fun dumpJson(obj: Any, file: File, callback: (Boolean) -> Unit) {
        Observable.create<Unit> {
            Gson().toJson(obj, FileWriter(file))
            it.onNext(null)
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
            callback(true)
        }, {
            callback(false)
            Log.e("johnson", it.message, it)
        })
    }
}
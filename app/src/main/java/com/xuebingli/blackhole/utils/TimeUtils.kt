package com.xuebingli.blackhole.utils

import android.os.SystemClock

class TimeUtils {
    fun getTimeStampAccurate(): Long {
        return SystemClock.elapsedRealtime()
    }

    fun getTimeStamp(): Long {
        return System.currentTimeMillis()
    }
}

fun getTimeStampAccurate(): Long {
    return SystemClock.elapsedRealtime()
}

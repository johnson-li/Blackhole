package com.xuebingli.blackhole.utils

import android.os.SystemClock

class TimeUtils {
    fun elapsedRealTime(): Long {
        return SystemClock.elapsedRealtime()
    }
}
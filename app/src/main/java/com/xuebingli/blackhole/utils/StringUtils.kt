package com.xuebingli.blackhole.utils

import android.util.Patterns
import com.xuebingli.blackhole.utils.Constants.Companion.G
import com.xuebingli.blackhole.utils.Constants.Companion.K
import com.xuebingli.blackhole.utils.Constants.Companion.M

fun isValidIpAddress(value: String): Boolean {
    return Patterns.IP_ADDRESS.matcher(value).matches()
}

fun getBitrateString(bitrate: Int): String {
    return when {
        bitrate >= Constants.G -> "%.1f gbps".format(bitrate.toFloat() / G)
        bitrate >= Constants.M -> "${bitrate / M} mbps"
        bitrate >= Constants.K -> "${bitrate / K} kbps"
        else -> "$bitrate bps"
    }
}

fun getDurationString(duration: Int): String {
    return when {
        duration == Int.MAX_VALUE -> "Infinity"
        duration >= 60 * 60 -> "${duration / 60 / 60} h"
        duration >= 60 -> "${duration / 60} min"
        else -> "$duration s"
    }
}

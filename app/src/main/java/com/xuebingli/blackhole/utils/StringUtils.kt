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
        bitrate >= G -> (bitrate.toFloat() / G)
            .let { (if (bitrate % G != 0) "%.2f Gbps" else "%.0f Gbps").format(it) }
        bitrate >= M -> (bitrate.toFloat() / M)
            .let { (if (bitrate % M != 0) "%.2f Mbps" else "%.0f Mbps").format(it) }
        bitrate >= K -> (bitrate.toFloat() / K)
            .let { (if (bitrate % K != 0) "%.2f Kbps" else "%.0f Kbps").format(it) }
        else -> "$bitrate bps"
    }
}

fun getProbingDelayString(delay: Int): String {
    return "$delay ms"
}

fun getDurationString(duration: Int): String {
    return when {
        duration == Int.MAX_VALUE -> "Infinity"
        duration >= 60 * 60 -> "${duration / 60 / 60} h"
        duration >= 60 -> "${duration / 60} min"
        else -> "$duration s"
    }
}

fun getSizeString(bytes: Int): String {
    return when {
        bytes >= G -> (bytes.toFloat() / G)
            .let { (if (bytes % G != 0) "%.2f GB" else "%.0f GB").format(it) }
        bytes >= M -> (bytes.toFloat() / M)
            .let { (if (bytes % M != 0) "%.2f MB" else "%.0f MB").format(it) }
        bytes >= K -> (bytes.toFloat() / K)
            .let { (if (bytes % K != 0) "%.2f KB" else "%.0f KB").format(it) }
        else -> "$bytes Bytes"
    }
}
package com.xuebingli.blackhole.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.xuebingli.blackhole.BuildConfig
import com.xuebingli.blackhole.utils.Preferences.Companion.CLOCK_DRIFT_KEY
import com.xuebingli.blackhole.utils.Preferences.Companion.DURATION_KEY
import com.xuebingli.blackhole.utils.Preferences.Companion.PACKET_SIZE_KEY
import com.xuebingli.blackhole.utils.Preferences.Companion.POUR_BITRATE_KEY
import com.xuebingli.blackhole.utils.Preferences.Companion.POUR_MODE_KEY
import com.xuebingli.blackhole.utils.Preferences.Companion.PREFERENCE_NAME
import com.xuebingli.blackhole.utils.Preferences.Companion.SINK_BITRATE_KEY
import com.xuebingli.blackhole.utils.Preferences.Companion.SINK_MODE_KEY
import com.xuebingli.blackhole.utils.Preferences.Companion.TARGET_IP_KEY
import java.io.File

class ConfigUtils(private val context: Context) {
    var clockDrift: Long
        get() {
            return getSharedPreferences().getLong(CLOCK_DRIFT_KEY, 0)
        }
        set(value) {
            getSharedPreferences().edit(true) { putLong(CLOCK_DRIFT_KEY, value) }
        }


    fun getSharedPreferences(): SharedPreferences {
        return context.applicationContext
            .getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    }

    fun getTargetIP(): String {
        val targetIP = getSharedPreferences().getString(TARGET_IP_KEY, null)
        return targetIP ?: BuildConfig.TARGET_IP
    }

    fun getSyncPort(): Int {
        return 3434
    }

    fun getTargetPort(): Int {
        return BuildConfig.TARGET_PORT
    }

    fun getSinkMode(): SinkMode {
        val sinkMode = getSharedPreferences().getString(SINK_MODE_KEY, null)
        return SinkMode.valueOf(sinkMode ?: BuildConfig.SINK_MODE)
    }

    fun getPourMode(): PourMode {
        val pourMode = getSharedPreferences().getString(POUR_MODE_KEY, null)
        return PourMode.valueOf(pourMode ?: BuildConfig.POUR_MODE)
    }

    fun getSinkBitrate(): Int {
        return getSharedPreferences().getInt(SINK_BITRATE_KEY, 1024 * 1024)
    }

    fun getPourBitrate(): Int {
        return getSharedPreferences().getInt(POUR_BITRATE_KEY, 1024 * 1024)
    }

    fun getPacketSize(): Int {
        return getSharedPreferences().getInt(PACKET_SIZE_KEY, 1024)
    }

    fun getDuration(): Int {
        return getSharedPreferences().getInt(DURATION_KEY, 15)
    }

    fun getDataDir(): File {
        return context.getExternalFilesDir(null)!!
    }
}

enum class SinkMode {
    UDP, TCP
}

enum class PourMode {
    UDP, TCP
}

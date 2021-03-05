package com.xuebingli.blackhole.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.xuebingli.blackhole.BuildConfig
import com.xuebingli.blackhole.utils.Preferences.Companion.CLIENT_ID_KEY
import com.xuebingli.blackhole.utils.Preferences.Companion.CLOCK_CONFIDENCE_KEY
import com.xuebingli.blackhole.utils.Preferences.Companion.CLOCK_DRIFT_KEY
import com.xuebingli.blackhole.utils.Preferences.Companion.DURATION_KEY
import com.xuebingli.blackhole.utils.Preferences.Companion.PACKET_SIZE_KEY
import com.xuebingli.blackhole.utils.Preferences.Companion.POUR_BITRATE_KEY
import com.xuebingli.blackhole.utils.Preferences.Companion.POUR_MODE_KEY
import com.xuebingli.blackhole.utils.Preferences.Companion.PREFERENCE_NAME
import com.xuebingli.blackhole.utils.Preferences.Companion.PROBING_DELAY_KEY
import com.xuebingli.blackhole.utils.Preferences.Companion.SINK_BITRATE_KEY
import com.xuebingli.blackhole.utils.Preferences.Companion.SINK_MODE_KEY
import com.xuebingli.blackhole.utils.Preferences.Companion.TARGET_IP_KEY
import java.io.File
import java.util.*

class ConfigUtils(private val context: Context) {
    var clockDrift: Long
        get() {
            return getSharedPreferences().getLong(CLOCK_DRIFT_KEY, 0)
        }
        set(value) {
            getSharedPreferences().edit(true) { putLong(CLOCK_DRIFT_KEY, value) }
        }
    var clockConfidence: Float
        get() {
            return getSharedPreferences().getFloat(CLOCK_CONFIDENCE_KEY, 0f)
        }
        set(value) {
            getSharedPreferences().edit(true) { putFloat(CLOCK_CONFIDENCE_KEY, value) }
        }
    var targetIP: String
        get() {
            return getSharedPreferences().getString(TARGET_IP_KEY, BuildConfig.TARGET_IP)!!
        }
        set(value) {
            getSharedPreferences().edit(true) { putString(TARGET_IP_KEY, value) }
        }
    var probingDelay: Int
        get() {
            return getSharedPreferences().getInt(PROBING_DELAY_KEY, 10)!!
        }
        set(value) {
            getSharedPreferences().edit(true) { putInt(PROBING_DELAY_KEY, value) }
        }
    val clientID: UUID
        get() {
            var str = getSharedPreferences().getString(CLIENT_ID_KEY, null)
            if (str == null) {
                str = UUID.randomUUID().toString()
                getSharedPreferences().edit {
                    putString(CLIENT_ID_KEY, str)
                }
            }
            return UUID.fromString(str)
        }


    fun getSharedPreferences(): SharedPreferences {
        return context.applicationContext
            .getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
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

    fun getSinkBitrateStr(): String {
        return getBitrateString(getSinkBitrate())
    }

    fun getPourBitrate(): Int {
        return getSharedPreferences().getInt(POUR_BITRATE_KEY, 1024 * 1024)
    }

    fun getPourBitrateStr(): String {
        return getBitrateString(getPourBitrate())
    }

    fun getPacketSize(): Int {
        return getSharedPreferences().getInt(PACKET_SIZE_KEY, 1500 - 8)
    }

    fun getPacketSizeStr(): String {
        return getSizeString(getPacketSize())
    }

    fun getDuration(): Int {
        return getSharedPreferences().getInt(DURATION_KEY, 15)
    }

    fun getDurationStr(): String {
        return getDurationString(getDuration())
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

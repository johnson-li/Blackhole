package com.xuebingli.blackhole.models

import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xuebingli.blackhole.utils.GsonUtils
import com.xuebingli.blackhole.utils.Preferences

open class GenericRecord {
    var createdAt = System.currentTimeMillis()
    var createdAtRealtime = SystemClock.elapsedRealtime()
}

class Records(
    val setup: MeasurementSetup,
    val records: List<GenericRecord>
)

class Measurement {
    companion object {
        fun loadSetup(pref: SharedPreferences): Measurement {
            return loadSetup(pref.getString(Preferences.MEASUREMENT_SETUP_KEY, null))
        }

        fun loadSetup(json: String?): Measurement {
            val measurement = Measurement()
            try {
                val setups = GsonUtils.getGson().fromJson<List<MeasurementSetup>>(
                    json,
                    object : TypeToken<List<MeasurementSetup>>() {}.type
                )
                setups.forEach(measurement::addMeasurement)
            } catch (e: java.lang.Exception) {
                Log.e("johnson", e.message, e)
            }
            return measurement
        }
    }

    var createdAt: Long = System.currentTimeMillis()
    var startedAt: Long = -1
    val recordSet: MutableMap<MeasurementSetup, Records> = mutableMapOf()

    val setups: List<MeasurementSetup>
        get() = recordSet.keys.toList().sortedBy { it.createdAt }

    fun addMeasurement(setup: MeasurementSetup): Boolean {
        if (setup.key.unique) {
            if (setups.any { it.key == setup.key }) {
                return false
            }
        }
        recordSet[setup] = when (setup.key) {
            MeasurementKey.LocationInfo -> Records(setup, mutableListOf<LocationRecord>())
            MeasurementKey.CellularInfo -> Records(setup, mutableListOf<CellularRecord>())
            MeasurementKey.Ping -> Records(setup, mutableListOf<PingRecord>())
            MeasurementKey.SubscriptionInfo -> Records(setup, mutableListOf<SubscriptionRecord>())
        }
        return true
    }

    fun saveSetup(pref: SharedPreferences) {
        pref.edit {
            val data = GsonUtils.getGson().toJson(setups)
            putString(Preferences.MEASUREMENT_SETUP_KEY, data)
            apply()
        }
    }

    fun empty(): Boolean {
        return recordSet.isEmpty()
    }
}

abstract class MeasurementSetup(val key: MeasurementKey) {
    var createdAt = System.currentTimeMillis()
    var updatedAt = createdAt

    override fun toString(): String {
        return "${key.name}@$createdAt"
    }
}


enum class MeasurementKey(val unique: Boolean) {
    SubscriptionInfo(true),
    CellularInfo(true),
    LocationInfo(true),
    Ping(false),
}
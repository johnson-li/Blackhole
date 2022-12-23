package com.xuebingli.blackhole.models

import android.content.SharedPreferences
import android.os.SystemClock
import android.text.TextUtils
import android.util.Log
import androidx.core.content.edit
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xuebingli.blackhole.BR
import com.xuebingli.blackhole.utils.GsonUtils
import com.xuebingli.blackhole.utils.Preferences

abstract class GenericRecord {
    var createdAt = System.currentTimeMillis()
    var createdAtRealtime = SystemClock.elapsedRealtime()

    abstract fun toUiString(): String
}

class Records(
    val setup: MeasurementSetup,
    val records: MutableList<GenericRecord>
) : BaseObservable() {
    @get:Bindable
    val lastRecord: GenericRecord?
        get() = if (records.isEmpty()) null else records.last()

    @get:Bindable
    val recordSize: Int
        get() = records.size

    fun appendRecord(record: GenericRecord) {
        records.add(record)
        notifyPropertyChanged(BR.lastRecord)
        notifyPropertyChanged(BR.recordSize)
    }
}

class Measurement : BaseObservable() {
    companion object {
        fun loadSetup(pref: SharedPreferences): Measurement {
            return loadSetup(pref.getString(Preferences.MEASUREMENT_SETUP_KEY, null))
        }

        private fun loadSetup(json: String?): Measurement {
            val measurement = Measurement()
            try {
                if (!TextUtils.isEmpty(json)) {
                    val setups = GsonUtils.getGson().fromJson<List<MeasurementSetup>>(
                        json, object : TypeToken<List<MeasurementSetup>>() {}.type
                    )
                    setups.forEach(measurement::addMeasurement)
                }
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

    @get:Bindable
    val empty: Boolean
        get() = recordSet.isEmpty()

    fun addMeasurement(setup: MeasurementSetup): Boolean {
        if (setup.key.unique) {
            if (setups.any { it.key == setup.key }) {
                return false
            }
        }
        recordSet[setup] = Records(setup, mutableListOf())
        notifyPropertyChanged(BR.empty)
        return true
    }

    fun removeMeasurement(setup: MeasurementSetup) {
        recordSet.remove(setup)
        notifyPropertyChanged(BR.empty)
    }

    fun saveSetup(pref: SharedPreferences) {
        pref.edit {
            val data = GsonUtils.getGson().toJson(setups)
            putString(Preferences.MEASUREMENT_SETUP_KEY, data)
            apply()
        }
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
    NetworkInfo(true),
    LocationInfo(true),
    Ping(false),
    UdpPing(false),
}
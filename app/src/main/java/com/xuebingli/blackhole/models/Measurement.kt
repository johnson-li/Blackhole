package com.xuebingli.blackhole.models

import android.os.SystemClock

open class GenericRecord {
    var ts = System.currentTimeMillis()
    var tsRealtime = SystemClock.elapsedRealtime()
}

class Records(
    val records: List<GenericRecord> = mutableListOf()
)

class Measurement {
    companion object {
        fun loadSetup(json: String?): Measurement {
            return Measurement()
        }
    }

    var startedAt: Long = System.currentTimeMillis()
    val recordSet: MutableMap<MeasurementSetup, Records> = mutableMapOf()

    val setups: List<MeasurementSetup>
        get() = recordSet.keys.toList().sortedBy { it.ts }
}

open class MeasurementSetup(val key: MeasurementKey) {
    var ts = System.currentTimeMillis()
}


enum class MeasurementKey {
    CellularInfo,
    LocationInfo,
    Ping,
}
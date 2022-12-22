package com.xuebingli.blackhole.models

class PingMeasurementSetup : MeasurementSetup(MeasurementKey.Ping)

class PingRecord : GenericRecord() {
    override fun toUiString(): String {
        return ""
    }
}
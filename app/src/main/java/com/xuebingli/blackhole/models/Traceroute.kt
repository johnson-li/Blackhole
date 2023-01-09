package com.xuebingli.blackhole.models

class TracerouteMeasurementSetup : MeasurementSetup(MeasurementKey.Traceroute) {
    var serverIP: String = "195.148.127.230"
    var interval: Int = 1000

    override fun description(): String = "Target: $serverIP"
}

class TracerouteRecord : GenericRecord() {
    override fun toUiString0(): String {
        return ""
    }
}

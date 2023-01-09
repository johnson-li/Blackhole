package com.xuebingli.blackhole.models

class TracerouteMeasurementSetup : MeasurementSetup(MeasurementKey.Traceroute) {
    var serverIP: String = "195.148.127.230"
    var interval: Int = 1000
}

class TracerouteRecord : GenericRecord() {
    override fun toUiString(): String {
        return ""
    }
}

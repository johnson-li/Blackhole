package com.xuebingli.blackhole.models

class PingMeasurementSetup : MeasurementSetup(MeasurementKey.Ping)

class PingRecord : GenericRecord() {
    override fun toUiString(): String {
        return ""
    }
}

class UdpPingMeasurementSetup : MeasurementSetup(MeasurementKey.UdpPing) {
    var serverIP: String = "195.148.127.230"
    var serverPort: Int = 8877
    var interval: Int = 10
}

class UdpPingRecord : GenericRecord() {
    override fun toUiString(): String {
        return ""
    }
}

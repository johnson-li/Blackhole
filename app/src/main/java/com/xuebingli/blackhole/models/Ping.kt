package com.xuebingli.blackhole.models

class PingMeasurementSetup : MeasurementSetup(MeasurementKey.Ping) {
    var serverIP: String = "195.148.127.230"
    var interval: Int = 200

    override fun description(): String = "@$serverIP"
}

class PingRecord(private val rtt: Float) : GenericRecord() {
    override fun toUiString0(): String = "RTT: $rtt ms"
}

class UdpPingMeasurementSetup : MeasurementSetup(MeasurementKey.UdpPing) {
    var serverIP: String = "195.148.127.234"
    var serverPort: Int = 8085
    var interval: Int = 100

    override fun description(): String = "@$serverIP:$serverPort"
}

class UdpPingRecord(val pktId: Int, val sendTs: Long, val recvTs: Long) : GenericRecord() {
    override fun toUiString0(): String = "RTT: ${recvTs - sendTs} ms"
}

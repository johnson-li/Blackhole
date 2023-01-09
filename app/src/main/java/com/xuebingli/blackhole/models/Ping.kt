package com.xuebingli.blackhole.models

class PingMeasurementSetup : MeasurementSetup(MeasurementKey.Ping)

class PingRecord : GenericRecord() {
    override fun toUiString0(): String = ""
}

class UdpPingMeasurementSetup : MeasurementSetup(MeasurementKey.UdpPing) {
    var serverIP: String = "195.148.127.230"
    var serverPort: Int = 8877
    var interval: Int = 100

    override fun description(): String = "Target: $serverIP:$serverPort"
}

class UdpPingRecord(val pktId: Int, val sendTs: Long, val recvTs: Long) : GenericRecord() {
    override fun toUiString0(): String = "RTT: ${recvTs - sendTs}ms"
}

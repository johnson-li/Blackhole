package com.xuebingli.blackhole.restful

import com.google.gson.annotations.SerializedName

data class HttpResponse(
    val status: String
)

data class Response(
    val id: String,
    val status: Status,
    val message: String?,
    val type: RequestType,
    val protocol: Protocol?,
    val port: Int?,
    val statics: Statics?
)

data class Statics(
    val udp_sink: Map<Int, PacketInfo>?,
    val udp_pour: Map<Int, PacketInfo>?,
    val tcp_sink: Map<Int, PacketInfo>?,
    val tcp_pour: Map<Int, PacketInfo>?,
    val probing_sent: List<ProbingRecord>,
    val probing_received: List<ProbingRecord>,
)

data class PacketInfo(
    val timestamp: Long,
    val size: Int?
)

enum class Status {
    @SerializedName("1")
    SUCCESS,

    @SerializedName("-1")
    FAIL
}

enum class Protocol {
    TCP,
    UDP
}
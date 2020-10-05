package com.xuebingli.blackhole.restful

import com.google.gson.annotations.SerializedName

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
    val udp_pour: Map<Int, PacketInfo>?
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
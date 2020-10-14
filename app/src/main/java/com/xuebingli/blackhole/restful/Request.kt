package com.xuebingli.blackhole.restful

import com.google.gson.annotations.SerializedName

data class Request(
    var type: RequestType,
    var bitrate: Int = 0,
)

data class PourRequest(
    var id: String,
    var command: String,
    @SerializedName("packet_size")
    var packetSize: Int? = null,
    var bitrate: Int? = null,
    var duration: Int? = null,
    @SerializedName("data_size")
    var dataSize: Long? = null,
)

enum class RequestType {
    @SerializedName("udp_sink")
    UDP_SINK,

    @SerializedName("udp_pour")
    UDP_POUR,

    @SerializedName("tcp_pour")
    TCP_POUR,

    @SerializedName("tcp_sink")
    TCP_SINK,

    @SerializedName("udp_echo")
    UDP_ECHO,

    @SerializedName("statics")
    STATICS,

    @SerializedName("cleanup")
    CLEANUP,
}


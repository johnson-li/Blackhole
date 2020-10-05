package com.xuebingli.blackhole.restful

import com.google.gson.annotations.SerializedName

data class Request(
    var type: RequestType,
    var bitrate: Int = 0
)

data class PourRequest(
    var id: String,
    var command: String,
    @SerializedName("packet_size")
    var packetSize: Int,
    var bitrate: Int,
    var duration: Int
)

enum class RequestType {
    @SerializedName("udp_sink")
    UDP_SINK,

    @SerializedName("udp_pour")
    UDP_POUR,

    @SerializedName("udp_echo")
    UDP_ECHO,

    @SerializedName("statics")
    STATICS,
}


package com.xuebingli.blackhole.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PacketReport(
    @SerialName("seq")
    val sequence: Int? = null,
    val size: Int,
    @SerialName("localTs")
    val localTimestamp: Long? = null,
    @SerialName("remoteTs")
    var remoteTimestamp: Long? = null,
    var timestamp: Long? = null
)

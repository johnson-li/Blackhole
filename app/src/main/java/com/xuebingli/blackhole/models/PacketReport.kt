package com.xuebingli.blackhole.models

import com.google.gson.annotations.SerializedName

data class PacketReport(
    @SerializedName("seq")
    val sequence: Int,
    val size: Int,
    @SerializedName("localTs")
    val localTimestamp: Long,
    @SerializedName("remoteTs")
    var remoteTimestamp: Long? = null
)

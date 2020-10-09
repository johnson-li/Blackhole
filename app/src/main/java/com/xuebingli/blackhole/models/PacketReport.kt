package com.xuebingli.blackhole.models

data class PacketReport(
    val sequence: Int,
    val size: Int,
    val localTimestamp: Long,
    var remoteTimestamp: Long? = null
)

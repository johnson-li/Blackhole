package com.xuebingli.blackhole.restful

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

enum class ProbingRecordType {
    @SerializedName("sent")
    SENT,

    @SerializedName("received")
    RECEIVED,
}

data class ProbingRecord(
    var timestamp: Long,
    var sequence: Int,
    var type: ProbingRecordType,
)

data class ProbingResult0(
    val received: MutableList<ProbingRecord> = mutableListOf(),
    val sent: MutableList<ProbingRecord> = mutableListOf(),
)

data class ProbingResult(
    val clientResult: ProbingResult0 = ProbingResult0(),
    val serverResult: ProbingResult0 = ProbingResult0(),
)
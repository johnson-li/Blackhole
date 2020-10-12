package com.xuebingli.blackhole.models

import android.content.Context
import kotlinx.serialization.Serializable

fun getReport(context: Context? = null, packetReports: List<PacketReport>?): Report {
    return Report(
        packetReports = packetReports,
        preferenceReports = context?.run { getPreferenceReportList(this) })
}

@Serializable
data class Report(
    val packetReports: List<PacketReport>? = null,
    val preferenceReports: List<PreferenceReport>? = null,
)
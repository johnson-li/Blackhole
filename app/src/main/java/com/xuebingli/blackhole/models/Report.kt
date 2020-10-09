package com.xuebingli.blackhole.models

import android.content.Context

fun getReport(context: Context? = null, packetReports: List<PacketReport>?): Report {
    return Report(
        packetReports = packetReports,
        preferenceReports = context?.run { getPreferenceReportList(this) })
}

data class Report(
    val packetReports: List<PacketReport>? = null,
    val preferenceReports: List<PreferenceReport>? = null,
)
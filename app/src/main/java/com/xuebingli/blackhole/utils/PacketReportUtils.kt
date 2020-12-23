package com.xuebingli.blackhole.utils

import com.xuebingli.blackhole.models.PacketReport
import com.xuebingli.blackhole.restful.PacketInfo

class PacketReportUtils {
    companion object {
        fun update(reports: ArrayList<PacketReport>, statics: Map<Int, PacketInfo>) {
            if (reports.isNotEmpty()) {
                for (report in reports) {
                    val sequence = report.sequence
                    statics[sequence]?.apply {
                        report.remoteTimestamp = timestamp
                    }
                }
            } else {
                for (seq in statics.keys.sorted()) {
                    val packetInfo = statics[seq]
                    reports.add(
                        PacketReport(
                            size = packetInfo!!.size!!,
                            remoteTimestamp = packetInfo.timestamp
                        )
                    )
                }
            }
        }
    }
}
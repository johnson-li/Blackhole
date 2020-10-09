package com.xuebingli.blackhole.utils

import com.xuebingli.blackhole.models.PacketReport
import com.xuebingli.blackhole.restful.PacketInfo

class PacketReportUtils {
    companion object {
        fun update(reports: List<PacketReport>, statics: Map<Int, PacketInfo>) {
            for (report in reports) {
                val sequence = report.sequence
                statics[sequence]?.apply {
                    report.remoteTimestamp = timestamp
                }
            }
        }
    }
}
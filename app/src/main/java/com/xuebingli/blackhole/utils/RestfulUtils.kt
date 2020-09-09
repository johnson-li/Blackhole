package com.xuebingli.blackhole.utils

import com.xuebingli.blackhole.restful.ControlMessage
import com.xuebingli.blackhole.restful.Request
import com.xuebingli.blackhole.restful.RequestType
import java.util.*

fun buildControlMessage(requestType: RequestType, bitrate: Int): ControlMessage {
    return ControlMessage(
        UUID.randomUUID().toString(),
        Request(
            requestType,
            bitrate
        )
    )
}

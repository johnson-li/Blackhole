package com.xuebingli.blackhole.utils

import java.net.NetworkInterface

fun getInterfaces(): Array<String> {
    val interfaces = ArrayList<String>()
    val networkInterfaces = NetworkInterface.getNetworkInterfaces()
    for (iface in networkInterfaces) {
        interfaces.add(iface.displayName)
    }
    return interfaces.toTypedArray()
}
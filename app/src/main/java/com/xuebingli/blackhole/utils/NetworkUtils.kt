package com.xuebingli.blackhole.utils

import android.net.ConnectivityManager
import android.net.Network
import java.lang.reflect.Method
import java.net.InetAddress
import java.net.NetworkInterface

fun getInterfaces(): Array<String> {
    val interfaces = ArrayList<String>()
    val networkInterfaces = NetworkInterface.getNetworkInterfaces()
    for (iface in networkInterfaces) {
        interfaces.add(iface.displayName)
    }
    return interfaces.toTypedArray()
}

fun getDnsServers(connectivityManager: ConnectivityManager): List<InetAddress> {
    return connectivityManager.activeNetwork?.let {
        connectivityManager.getLinkProperties(it)?.dnsServers
    } ?: listOf()
}
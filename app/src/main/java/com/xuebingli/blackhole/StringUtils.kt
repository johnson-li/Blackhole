package com.xuebingli.blackhole

import android.util.Patterns

fun isValidIpAddress(value: String) : Boolean {
    return Patterns.IP_ADDRESS.matcher(value).matches()
}

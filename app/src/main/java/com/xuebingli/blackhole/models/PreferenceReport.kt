package com.xuebingli.blackhole.models

import android.content.Context
import com.xuebingli.blackhole.utils.ConfigUtils

fun getPreferenceReportList(context: Context): List<PreferenceReport> {
    val preference = ConfigUtils(context).getSharedPreferences()
    return preference.all.map {
        getPreferenceReport(it.key, it.value ?: "null")
    }
}

fun getPreferenceReport(key: String, value: Any): PreferenceReport {
    return PreferenceReport(key = key, value = value, valueType = value.javaClass.name)
}

data class PreferenceReport(
    val key: String,
    val value: Any,
    val valueType: String
)
package com.xuebingli.blackhole.models

import android.content.Context
import com.xuebingli.blackhole.utils.ConfigUtils
import kotlinx.serialization.Serializable

fun getPreferenceReportList(context: Context): List<PreferenceReport> {
    val preference = ConfigUtils(context).getSharedPreferences()
    return preference.all.map {
        getPreferenceReport(it.key, it.value ?: "null")
    }
}

fun getPreferenceReport(key: String, value: Any): PreferenceReport {
    return PreferenceReport(key = key, value = value.toString(), valueType = value.javaClass.name)
}

@Serializable
data class PreferenceReport(
    val key: String,
    val value: String,
    val valueType: String
)
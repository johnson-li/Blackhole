package com.xuebingli.blackhole.models

data class GpsLocation(
    val time: Long,
    val localTime: Long,
    val monoTime: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float
)
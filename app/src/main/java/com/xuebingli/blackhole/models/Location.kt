package com.xuebingli.blackhole.models

class LocationMeasurementSetup : MeasurementSetup(MeasurementKey.LocationInfo)

class LocationRecord : GenericRecord() {
    var latitude = .0
    val longitude = .0
    val accuracy = .0
}
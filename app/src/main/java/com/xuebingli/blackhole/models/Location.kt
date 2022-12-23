package com.xuebingli.blackhole.models

class LocationMeasurementSetup : MeasurementSetup(MeasurementKey.LocationInfo)

class LocationRecord(val latitude: Double, val longitude: Double, val accuracy: Double) :
    GenericRecord() {
    override fun toUiString(): String {
        return "GPS: ($latitude, $longitude), Accuracy: $accuracy"
    }

    override fun equals(other: Any?): Boolean {
        return other is LocationRecord && (other.latitude == latitude &&
                other.longitude == longitude && other.accuracy == accuracy)
    }
}
package com.xuebingli.blackhole.models

class LocationMeasurementSetup : MeasurementSetup(MeasurementKey.LocationInfo)

class LocationRecord(val latitude: Double, val longitude: Double, val accuracy: Double) :
    GenericRecord()
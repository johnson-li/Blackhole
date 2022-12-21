package com.xuebingli.blackhole.models

class CellularMeasurementSetup : MeasurementSetup(MeasurementKey.CellularInfo)

class CellularRecord : GenericRecord() {
    var type = CellInfoType.UNKNOWN
}
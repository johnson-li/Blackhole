package com.xuebingli.blackhole.models

class CellularMeasurementSetup : MeasurementSetup(MeasurementKey.CellularInfo)

class CellularRecord(private val cellInfo: CellInfoModel) : GenericRecord() {
    override fun toUiString(): String {
        return "${cellInfo.cellInfoType}"
    }
}


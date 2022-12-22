package com.xuebingli.blackhole.models

class CellularMeasurementSetup : MeasurementSetup(MeasurementKey.CellularInfo)

class CellularRecord(val cellInfo: CellInfoModel) : GenericRecord()


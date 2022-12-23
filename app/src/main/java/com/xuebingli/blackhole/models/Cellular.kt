package com.xuebingli.blackhole.models

class CellularMeasurementSetup : MeasurementSetup(MeasurementKey.CellularInfo)

class CellularRecord(private val cellInfo: CellInfoModel) : GenericRecord() {
    override fun toUiString(): String {
        return "${cellInfo.cellInfoType}, RSSI: ${cellInfo.signalStrength?.rssi}, " +
                "RSRP: ${cellInfo.signalStrength?.rsrp}, RSRQ: ${cellInfo.signalStrength?.rsrq}"
    }
}

class NetworkInfoMeasurementSetup: MeasurementSetup(MeasurementKey.NetworkInfo)

class NetworkInfoRecord(private val networkInfo: CellNetworkInfo): GenericRecord() {
    override fun toUiString(): String = "Downlink: ${networkInfo.downLink}, " +
            "Uplink: ${networkInfo.upLink}"

}
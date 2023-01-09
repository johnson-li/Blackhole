package com.xuebingli.blackhole.models

class CellularMeasurementSetup : MeasurementSetup(MeasurementKey.CellularInfo)

class CellularRecord(private val cellInfo: CellInfoModel) : GenericRecord() {
    override fun toUiString0(): String {
        return "${cellInfo.cellInfoType}, RSSI: ${cellInfo.signalStrength?.rssi}, " +
                "RSRP: ${cellInfo.signalStrength?.rsrp}, RSRQ: ${cellInfo.signalStrength?.rsrq}"
    }

    override fun equals(other: Any?): Boolean {
        return other is CellularRecord && other.cellInfo == cellInfo
    }
}

class NetworkInfoMeasurementSetup: MeasurementSetup(MeasurementKey.NetworkInfo)

class NetworkInfoRecord(private val networkInfo: CellNetworkInfo): GenericRecord() {
    override fun toUiString0(): String = "Downlink: ${networkInfo.downLink}, " +
            "Uplink: ${networkInfo.upLink}"

    override fun equals(other: Any?): Boolean {
        return other is NetworkInfoRecord && other.networkInfo == networkInfo
    }
}
package com.xuebingli.blackhole.models

import android.os.Build
import android.telephony.*
import android.util.Log

fun getCellInfoModel(cellInfo: CellInfo): CellInfoModel {
    return when (cellInfo) {
        is CellInfoNr -> {
            CellInfoModel(
                cellInfoType = CellInfoType.NR,
                connectionStatus = getCellInfoConnectionStatus(cellInfo.cellConnectionStatus),
                identity = (cellInfo.cellIdentity as CellIdentityNr).run {
                    CellInfoCellIdentity(
                        cellInfoType = CellInfoType.LTE,
                        pci = pci,
                        tac = tac,
                        nci = nci,
                        nrArfcn = nrarfcn,
                        bands = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) bands.asList() else null,
                        mcc = mccString,
                        mnc = mncString,
                        alphaLong = operatorAlphaLong.toString(),
                        alphaShort = operatorAlphaShort.toString()
                    )
                },
                signalStrength = (cellInfo.cellSignalStrength as CellSignalStrengthNr).run {
                    CellInfoSignalStrength(
                        cellInfoType = CellInfoType.NR,
                        csiRsrp = csiRsrp,
                        csiRsrq = csiRsrq,
                        csiSinr = csiSinr,
                        ssRsrp = ssRsrp,
                        ssRsrq = ssRsrq,
                        ssSinr = ssSinr,
                        level = level
                    )
                },
                isRegistered = cellInfo.isRegistered,
                timestamp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) cellInfo.timestampMillis else null
            )
        }
        is CellInfoLte -> {
            CellInfoModel(
                cellInfoType = CellInfoType.LTE,
                connectionStatus = getCellInfoConnectionStatus(cellInfo.cellConnectionStatus),
                identity = cellInfo.cellIdentity.run {
                    CellInfoCellIdentity(
                        cellInfoType = CellInfoType.LTE,
                        ci = ci,
                        pci = pci,
                        tac = tac,
                        earfcn = earfcn,
                        bands = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) bands.toList() else null,
                        bandwidth = bandwidth,
                        mcc = mccString,
                        mnc = mncString,
                        alphaShort = operatorAlphaShort.toString(),
                        alphaLong = operatorAlphaLong.toString()
                    )
                },
                signalStrength = (cellInfo.cellSignalStrength as CellSignalStrengthLte).run {
                    CellInfoSignalStrength(
                        cellInfoType = CellInfoType.LTE,
                        rssi = rssi,
                        rsrp = rsrp,
                        rsrq = rsrq,
                        rssnr = rssnr,
                        cqi = cqi,
                        level = level,
                        timingAdvance = timingAdvance
                    )
                },
                isRegistered = cellInfo.isRegistered,
                timestamp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) cellInfo.timestampMillis else null
            )
        }
        is CellInfoGsm -> {
            CellInfoModel(
                cellInfoType = CellInfoType.GSM,
                connectionStatus = getCellInfoConnectionStatus(cellInfo.cellConnectionStatus),
                identity = cellInfo.cellIdentity.run {
                    CellInfoCellIdentity(CellInfoType.GSM)
                },
                signalStrength = (cellInfo.cellSignalStrength as CellSignalStrengthGsm).run {
                    CellInfoSignalStrength(
                        rssi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) rssi else null,
                        level = level,
                        timingAdvance = timingAdvance,
                        errorRate = bitErrorRate
                    )
                },
                isRegistered = cellInfo.isRegistered,
                timestamp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) cellInfo.timestampMillis else null
            )
        }
        is CellInfoWcdma -> {
            CellInfoModel(
                cellInfoType = CellInfoType.WCDMA,
                connectionStatus = getCellInfoConnectionStatus(cellInfo.cellConnectionStatus),
                identity = cellInfo.cellIdentity.run {
                    CellInfoCellIdentity(CellInfoType.WCDMA)
                },
                signalStrength = (cellInfo.cellSignalStrength as CellSignalStrengthWcdma).run {
                    CellInfoSignalStrength(
                        rssi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) dbm else null,
                        level = level,
                        ecno = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ecNo else null
                    )
                },
                isRegistered = cellInfo.isRegistered,
                timestamp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) cellInfo.timestampMillis else null
            )
        }
        else -> {
            Log.w("johnson", "Ignore cell info of type ${cellInfo.javaClass.name}")
            CellInfoModel()
        }
    }
}

data class CellInfoCellIdentity(
    val cellInfoType: CellInfoType? = null,
    val ci: Int? = null,
    val pci: Int? = null,
    val tac: Int? = null,
    val nci: Long? = null,
    val earfcn: Int? = null,
    val nrArfcn: Int? = null,
    val bands: List<Int>? = null,
    val bandwidth: Int? = null,
    val mcc: String? = null,
    val mnc: String? = null,
    val alphaLong: String? = null,
    val alphaShort: String? = null
)

data class CellInfoSignalStrength(
    val cellInfoType: CellInfoType? = null,
    val level: Int? = null,
    val ecno: Int? = null,

    // for LTE
    val rssi: Int? = null,
    val rsrp: Int? = null,
    val rsrq: Int? = null,
    val rssnr: Int? = null,
    val errorRate: Int? = null,
    val cqi: Int? = null,
    val timingAdvance: Int? = null,
    val mParametersUseForLevel: Int? = null,

    // for NR
    val csiRsrp: Int? = null,
    val csiRsrq: Int? = null,
    val csiSinr: Int? = null,
    val ssRsrp: Int? = null,
    val ssRsrq: Int? = null,
    val ssSinr: Int? = null
)

data class CellInfoModel(
    val cellInfoType: CellInfoType? = null,
    val connectionStatus: CellInfoConnectionStatus? = null,
    val identity: CellInfoCellIdentity? = null,
    val signalStrength: CellInfoSignalStrength? = null,
    val isRegistered: Boolean? = false,
    val timestamp: Long? = null
)

enum class CellInfoType {
    NR, LTE, GSM, WCDMA, OTHER
}

fun getCellInfoConnectionStatus(value: Int): CellInfoConnectionStatus {
    return when (value) {
        CellInfoConnectionStatus.CONNECTION_NONE.value -> CellInfoConnectionStatus.CONNECTION_NONE
        CellInfoConnectionStatus.CONNECTION_PRIMARY_SERVING.value -> CellInfoConnectionStatus.CONNECTION_PRIMARY_SERVING
        CellInfoConnectionStatus.CONNECTION_SECONDARY_SERVING.value -> CellInfoConnectionStatus.CONNECTION_SECONDARY_SERVING
        CellInfoConnectionStatus.CONNECTION_UNKNOWN.value -> CellInfoConnectionStatus.CONNECTION_UNKNOWN
        else -> CellInfoConnectionStatus.CONNECTION_UNKNOWN
    }
}

enum class CellInfoConnectionStatus(val value: Int) {
    CONNECTION_NONE(CellInfo.CONNECTION_NONE),
    CONNECTION_PRIMARY_SERVING(CellInfo.CONNECTION_PRIMARY_SERVING),
    CONNECTION_SECONDARY_SERVING(CellInfo.CONNECTION_SECONDARY_SERVING),
    CONNECTION_UNKNOWN(CellInfo.CONNECTION_UNKNOWN)
}
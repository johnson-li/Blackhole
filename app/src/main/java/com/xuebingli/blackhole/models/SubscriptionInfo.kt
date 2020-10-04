package com.xuebingli.blackhole.models

import android.telephony.SubscriptionInfo

fun getSubscriptionInfoModel(subscriptionInfo: SubscriptionInfo): SubscriptionInfoModel {
    return subscriptionInfo.run {
        SubscriptionInfoModel(
            subscriptionId = subscriptionId,
            iccId = iccId,
            simSlotIndex = simSlotIndex,
            displayName = displayName.toString(),
            carrierName = carrierName.toString(),
            carrierId = carrierId,
            number = number,
            dataRoaming = dataRoaming,
            mcc = mccString,
            mnc = mncString,
            countryIso = countryIso,
            isEmbedded = isEmbedded
        )
    }
}

data class SubscriptionInfoModel(
    val subscriptionId: Int? = null,
    val iccId: String? = null, // ICCID is the identifier of the actual SIM card itself – i.e. an identifier for the SIM chip
    val simSlotIndex: Int? = null,
    val displayName: String? = null,
    val carrierName: String? = null,
    val carrierId: Int? = null,
    val number: String? = null,
    val dataRoaming: Int? = null,
    val mcc: String? = null,
    val mnc: String? = null,
    val countryIso: String? = null,
    val isEmbedded: Boolean? = null
)


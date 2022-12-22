package com.xuebingli.blackhole.models

class SubscriptionMeasurementSetup : MeasurementSetup(MeasurementKey.SubscriptionInfo)

class SubscriptionRecord(private val subscriptionInfo: SubscriptionInfoModel) : GenericRecord() {
    override fun toUiString(): String {
        return "MCC: ${subscriptionInfo.mcc}, MNC: ${subscriptionInfo.mnc}"
    }
}


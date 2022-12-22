package com.xuebingli.blackhole.models

class SubscriptionMeasurementSetup : MeasurementSetup(MeasurementKey.SubscriptionInfo)

class SubscriptionRecord(val subscriptionInfo: SubscriptionInfoModel) : GenericRecord()


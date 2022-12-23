package com.xuebingli.blackhole.utils

import com.google.gson.*
import com.xuebingli.blackhole.models.*
import java.lang.reflect.Type


class GsonUtils {
    companion object {
        fun getGson(): Gson {
            return GsonBuilder().registerTypeAdapter(
                MeasurementSetup::class.java,
                InterfaceAdapter()
            ).create()
        }
    }
}

internal class InterfaceAdapter : JsonDeserializer<MeasurementSetup> {
    @Throws(JsonParseException::class)
    override fun deserialize(
        elem: JsonElement,
        interfaceType: Type?,
        context: JsonDeserializationContext
    ): MeasurementSetup {
        val wrapper: JsonObject = elem as JsonObject
        return MeasurementKey.valueOf(wrapper.get("key").asString).let {
            context.deserialize(elem, it.measurementSetupClass.java)
        }
    }
}
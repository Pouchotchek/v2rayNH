package com.v2ray.ang.fmt

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.V2rayConfig
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.util.JsonUtil
import java.util.Locale

object CustomFmt : FmtBase() {
    fun protocolLabel(raw: String): String? {
        val outbounds = JsonUtil.parseString(raw)?.get("outbounds")
            ?.takeIf { it.isJsonArray }?.asJsonArray ?: return null
        val protocols = outbounds.mapNotNull { entry ->
            val outbound = entry.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            val protocol = outbound.get("protocol")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
                ?.asString?.uppercase(Locale.ROOT) ?: return@mapNotNull null
            if (EConfigType.entries.none { it.name == protocol && it.protocolScheme.isNotEmpty() }) return@mapNotNull null
            val tag = outbound.get("tag")?.takeIf { it.isJsonPrimitive }?.asString
            tag to protocol
        }
        return (protocols.firstOrNull { it.first == AppConfig.TAG_PROXY } ?: protocols.firstOrNull())?.second
    }

    /**
     * Parses a JSON string into a ProfileItem object.
     *
     * @param str the JSON string to parse
     * @return the parsed ProfileItem object, or null if parsing fails
     */
    fun parse(str: String): ProfileItem {
        val config = ProfileItem.create(EConfigType.CUSTOM)

        val fullConfig = JsonUtil.fromJson(str, V2rayConfig::class.java)
        val outbound = fullConfig?.getProxyOutbound()

        config.remarks = fullConfig?.remarks ?: System.currentTimeMillis().toString()
        config.server = outbound?.getServerAddress()
        config.serverPort = outbound?.getServerPort()?.toString()

        return config
    }
}
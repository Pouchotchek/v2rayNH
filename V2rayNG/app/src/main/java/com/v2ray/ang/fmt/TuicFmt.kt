package com.v2ray.ang.fmt

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.enums.NetworkType
import com.v2ray.ang.extension.idnHost
import com.v2ray.ang.extension.nullIfBlank
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.Utils
import java.net.URI

/**
 * TUIC v5 share link: tuic://UUID:PASSWORD@host:port?congestion_control=bbr&udp_relay_mode=native&alpn=h3&sni=...#remarks
 * The UUID is stored in [ProfileItem.username] and the password in [ProfileItem.password].
 */
object TuicFmt : FmtBase() {
    private val insecureKeys = arrayOf("allow_insecure", "allowInsecure", "insecure")
    private val zeroRttKeys = arrayOf("zero_rtt_handshake", "reduce_rtt")

    /**
     * Parses a TUIC URI string into a ProfileItem object.
     *
     * @param str the TUIC URI string to parse
     * @return the parsed ProfileItem object
     */
    fun parse(str: String): ProfileItem {
        val config = ProfileItem.create(EConfigType.TUIC)

        val uri = URI(Utils.fixIllegalUrl(str))
        config.remarks = Utils.decodeURIComponent(uri.fragment.orEmpty()).let { it.ifEmpty { "none" } }
        config.server = uri.idnHost
        config.serverPort = (if (uri.port == -1) AppConfig.DEFAULT_PORT else uri.port).toString()

        // The userinfo is "uuid:password"; split the raw form so an encoded ':' inside the password survives.
        val rawUserInfo = uri.rawUserInfo.orEmpty()
        val separator = rawUserInfo.indexOf(':')
        if (separator < 0) {
            config.username = Utils.decodeURIComponent(rawUserInfo)
            config.password = ""
        } else {
            config.username = Utils.decodeURIComponent(rawUserInfo.substring(0, separator))
            config.password = Utils.decodeURIComponent(rawUserInfo.substring(separator + 1))
        }

        config.security = AppConfig.TLS
        config.network = NetworkType.TUIC.type
        config.alpn = "h3"
        config.insecure = false
        config.zeroRtt = false

        if (!uri.rawQuery.isNullOrEmpty()) {
            val queryParam = getQueryParam(uri)

            config.congestionControl = queryParam["congestion_control"]?.nullIfBlank()
            config.udpRelayMode = queryParam["udp_relay_mode"]?.nullIfBlank()
            config.alpn = queryParam["alpn"]?.nullIfBlank() ?: "h3"
            config.sni = queryParam["sni"]
            config.insecure = insecureKeys.any { queryParam[it] == "1" || queryParam[it] == "true" }
            config.zeroRtt = zeroRttKeys.any { queryParam[it] == "1" || queryParam[it] == "true" }
            config.echConfigList = queryParam["ech"]
            config.pinnedCA256 = queryParam["pcs"]
        }

        return config
    }

    /**
     * Converts a ProfileItem object to a URI string (without the scheme).
     *
     * @param config the ProfileItem object to convert
     * @return the converted URI string
     */
    fun toUri(config: ProfileItem): String {
        val dicQuery = HashMap<String, String>()

        config.congestionControl?.nullIfBlank()?.let { dicQuery["congestion_control"] = it }
        config.udpRelayMode?.nullIfBlank()?.let { dicQuery["udp_relay_mode"] = it }
        config.alpn?.nullIfBlank()?.let { dicQuery["alpn"] = it }
        config.sni?.nullIfBlank()?.let { dicQuery["sni"] = it }
        dicQuery["allow_insecure"] = if (config.insecure == true) "1" else "0"
        if (config.zeroRtt == true) dicQuery["zero_rtt_handshake"] = "1"
        config.echConfigList?.nullIfBlank()?.let { dicQuery["ech"] = it }
        config.pinnedCA256?.nullIfBlank()?.let { dicQuery["pcs"] = it }

        // FmtBase.toUri encodes the whole userinfo, which would also encode the ':' separator.
        val userInfo = Utils.encodeURIComponent(config.username.orEmpty()) + ":" +
                Utils.encodeURIComponent(config.password.orEmpty())
        val query = "?" + dicQuery.toList().joinToString("&") { it.first + "=" + Utils.encodeURIComponent(it.second) }
        val host = Utils.getIpv6Address(HttpUtil.toIdnDomain(config.server.orEmpty()))

        return "$userInfo@$host:${config.serverPort}$query#${Utils.encodeURIComponent(config.remarks)}"
    }
}

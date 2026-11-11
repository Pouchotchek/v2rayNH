package com.v2ray.ang.fmt

import com.v2ray.ang.AppConfig
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.enums.NetworkType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TuicFmtTest {

    private val uuid = "b3a1c2d4-1111-2222-3333-444455556666"

    @Test
    fun parseFullUri() {
        val profile = TuicFmt.parse(
            "tuic://$uuid:secret@example.com:8443" +
                    "?congestion_control=bbr&udp_relay_mode=quic&alpn=h3&sni=example.org" +
                    "&allow_insecure=1&zero_rtt_handshake=1&pcs=test-pin#my%20node"
        )

        assertEquals(EConfigType.TUIC, profile.configType)
        assertEquals("my node", profile.remarks)
        assertEquals("example.com", profile.server)
        assertEquals("8443", profile.serverPort)
        assertEquals(uuid, profile.username)
        assertEquals("secret", profile.password)
        assertEquals("bbr", profile.congestionControl)
        assertEquals("quic", profile.udpRelayMode)
        assertEquals("h3", profile.alpn)
        assertEquals("example.org", profile.sni)
        assertEquals(true, profile.insecure)
        assertEquals(true, profile.zeroRtt)
        assertEquals("test-pin", profile.pinnedCA256)
        assertEquals(AppConfig.TLS, profile.security)
        assertEquals(NetworkType.TUIC.type, profile.network)
    }

    @Test
    fun missingPortUses443AndAlpnDefaultsToH3() {
        val profile = TuicFmt.parse("tuic://$uuid:secret@example.com/")

        assertEquals("443", profile.serverPort)
        assertEquals("h3", profile.alpn)
        assertEquals(false, profile.insecure)
        assertEquals(false, profile.zeroRtt ?: false)
    }

    @Test
    fun passwordContainingEncodedColonIsPreserved() {
        val profile = TuicFmt.parse("tuic://$uuid:pa%3Ass%40word@example.com:443")

        assertEquals(uuid, profile.username)
        assertEquals("pa:ss@word", profile.password)
    }

    @Test
    fun userInfoWithoutPasswordGivesEmptyPassword() {
        val profile = TuicFmt.parse("tuic://$uuid@example.com:443")

        assertEquals(uuid, profile.username)
        assertEquals("", profile.password)
    }

    @Test
    fun ipv6HostIsParsed() {
        val profile = TuicFmt.parse("tuic://$uuid:secret@[2001:db8::1]:4443")

        assertEquals("2001:db8::1", profile.server)
        assertEquals("4443", profile.serverPort)
    }

    @Test
    fun toUriRoundTrips() {
        val original = TuicFmt.parse(
            "tuic://$uuid:pa%3Ass@example.com:8443" +
                    "?congestion_control=new_reno&udp_relay_mode=native&alpn=h3&sni=example.org" +
                    "&allow_insecure=0&zero_rtt_handshake=1#node"
        )

        val uri = TuicFmt.toUri(original)
        val parsed = TuicFmt.parse(EConfigType.TUIC.protocolScheme + uri)

        assertTrue(uri.startsWith("$uuid:pa%3Ass@example.com:8443?"))
        assertEquals(original.username, parsed.username)
        assertEquals(original.password, parsed.password)
        assertEquals(original.server, parsed.server)
        assertEquals(original.serverPort, parsed.serverPort)
        assertEquals(original.congestionControl, parsed.congestionControl)
        assertEquals(original.udpRelayMode, parsed.udpRelayMode)
        assertEquals(original.alpn, parsed.alpn)
        assertEquals(original.sni, parsed.sni)
        assertEquals(original.insecure, parsed.insecure)
        assertEquals(original.zeroRtt, parsed.zeroRtt)
        assertEquals(original.remarks, parsed.remarks)
    }
}

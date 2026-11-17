package com.v2ray.ang.core

import com.google.gson.JsonParser
import com.v2ray.ang.dto.V2rayConfig.RoutingBean.RulesBean
import com.v2ray.ang.fmt.CustomFmt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CustomProfileTest {
    @Test
    fun `app routes replace imported rules and use available outbound tags`() {
        val config = JsonParser.parseString("""
            {
              "outbounds": [
                {"tag":"edge","protocol":"vless"},
                {"tag":"bypass","protocol":"freedom"}
              ],
              "routing": {
                "balancers":[{"tag":"pool","selector":["edge"]}],
                "rules":[{"type":"field","network":"tcp,udp","balancerTag":"pool"}]
              }
            }
        """).asJsonObject
        assertTrue(CoreConfigManager.hasCustomRoutingRules(config))
        CoreConfigManager.applyCustomProfileRouting(config, listOf(
            RulesBean(domain = listOf("example.test"), outboundTag = "direct"),
            RulesBean(port = "443", network = "udp", outboundTag = "block"),
            RulesBean(port = "53", outboundTag = "proxy"),
            RulesBean(port = "123", outboundTag = "missing"),
        ), "IPIfNonMatch")

        val routing = config.getAsJsonObject("routing")
        val rules = routing.getAsJsonArray("rules")
        assertEquals(3, rules.size())
        assertEquals("bypass", rules[0].asJsonObject.get("outboundTag").asString)
        assertEquals("block", rules[1].asJsonObject.get("outboundTag").asString)
        assertEquals("edge", rules[2].asJsonObject.get("outboundTag").asString)
        assertEquals("IPIfNonMatch", routing.get("domainStrategy").asString)
        assertEquals(1, routing.getAsJsonArray("balancers").size())
        assertEquals("blackhole", config.getAsJsonArray("outbounds")[2].asJsonObject.get("protocol").asString)
    }

    @Test
    fun `missing profile rules are detected and unknown app routes are skipped`() {
        val config = JsonParser.parseString("""
            {"outbounds":[{"protocol":"vless"}],"routing":{"domainStrategy":"AsIs","rules":[]}}
        """).asJsonObject
        assertFalse(CoreConfigManager.hasCustomRoutingRules(config))
        CoreConfigManager.applyCustomProfileRouting(config, listOf(RulesBean(outboundTag = "missing")), "IPIfNonMatch")
        assertEquals(0, config.getAsJsonObject("routing").getAsJsonArray("rules").size())
    }

    @Test
    fun `protocol label identifies proxy outbound in stored custom configuration`() {
        assertEquals("VLESS", CustomFmt.protocolLabel("""
            {"outbounds":[{"protocol":"freedom"},{"protocol":"hysteria"},{"tag":"proxy","protocol":"vless"}]}
        """))
        assertEquals("HYSTERIA", CustomFmt.protocolLabel("""{"outbounds":[{"protocol":"hysteria"}]}"""))
        assertNull(CustomFmt.protocolLabel("""{"outbounds":[{"protocol":"freedom"}]}"""))
        assertNull(CustomFmt.protocolLabel("""{"outbounds":null}"""))
    }
}

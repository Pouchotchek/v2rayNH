package com.v2ray.ang.ui.main

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MainServerRowModelsTest {
    @Test
    fun splitsOnlyLeadingCountryFlagsWithAnAccompanyingName() {
        assertEquals("🇸🇪" to "Server", splitServerFlag("🇸🇪 Server"))
        assertEquals("🇳🇱" to "Server", splitServerFlag("🇳🇱Server"))
        assertEquals("" to "🇸🇪", splitServerFlag("🇸🇪"))
        assertEquals("" to "Server 🇸🇪", splitServerFlag("Server 🇸🇪"))
        assertEquals("" to "⚡ Server", splitServerFlag("⚡ Server"))
    }
}

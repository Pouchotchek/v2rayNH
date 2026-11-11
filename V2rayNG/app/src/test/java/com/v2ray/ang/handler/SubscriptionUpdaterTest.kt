package com.v2ray.ang.handler

import com.v2ray.ang.dto.entities.SubscriptionItem
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class SubscriptionUpdaterTest {
    private val now = TimeUnit.DAYS.toMillis(7)
    private val subscription = SubscriptionItem(url = "https://example.com/sub")

    @Test
    fun refreshesNewSubscriptionsAndExistingSubscriptionsAtTheirDeadline() {
        assertTrue(SubscriptionUpdater.isDue(subscription, now))
        val deadline = now - TimeUnit.HOURS.toMillis(2)
        assertFalse(SubscriptionUpdater.isDue(subscription.copy(lastUpdated = deadline + 1), now))
        assertTrue(SubscriptionUpdater.isDue(subscription.copy(lastUpdated = deadline), now))
        assertFalse(SubscriptionUpdater.isDue(subscription.copy(lastUpdated = deadline, updateInterval = 180), now))
    }

    @Test
    fun skipsDisabledAndLocalGroups() {
        assertFalse(SubscriptionUpdater.isDue(subscription.copy(enabled = false), now))
        assertFalse(SubscriptionUpdater.isDue(subscription.copy(autoUpdate = false), now))
        assertFalse(SubscriptionUpdater.isDue(subscription.copy(url = ""), now))
    }
}

package com.v2ray.ang.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.SubscriptionUsageInfo
import com.v2ray.ang.extension.toTrafficString
import com.v2ray.ang.util.Utils

private const val MILLIS_PER_SECOND = 1000L
private const val EXPIRY_DATE_PATTERN = "dd.MM.yy"
private const val NO_EXPIRY_YEAR = 2099
private val SubscriptionActionWidth = 32.dp

internal fun subscriptionExpiryLabel(expireMillis: Long?): String =
    expireMillis?.takeIf {
        (Utils.formatTimestamp(it, "yyyy").toIntOrNull() ?: 0) < NO_EXPIRY_YEAR
    }?.let { Utils.formatTimestamp(it, EXPIRY_DATE_PATTERN) } ?: "∞"

/** Locale-neutral values the subscription information box renders. */
internal data class SubscriptionInfoUiModel(
    val title: String?,
    val usedBytes: Long,
    /** Traffic quota in bytes; 0 means unlimited. */
    val totalBytes: Long,
    /** Used share of the quota in 0..1; null when the quota is unlimited. */
    val trafficFraction: Float?,
    /** Expiry in epoch milliseconds; null when the subscription never expires. */
    val expireMillis: Long?,
    val expired: Boolean,
    val supportUrl: String?,
    val webPageUrl: String?,
) {
    val trafficLow: Boolean get() = trafficFraction != null && trafficFraction >= TRAFFIC_WARNING_FRACTION

    companion object {
        const val TRAFFIC_WARNING_FRACTION = 0.9f
    }
}

internal fun buildSubscriptionInfoUiModel(
    info: SubscriptionUsageInfo,
    nowMillis: Long,
): SubscriptionInfoUiModel {
    val unlimited = info.total <= 0L
    val expireMillis = info.expire.takeIf { it > 0L }?.let { it * MILLIS_PER_SECOND }
    return SubscriptionInfoUiModel(
        title = info.title,
        usedBytes = info.used,
        totalBytes = if (unlimited) 0L else info.total,
        trafficFraction = if (unlimited) null else (info.used.toDouble() / info.total).toFloat().coerceIn(0f, 1f),
        expireMillis = expireMillis,
        expired = expireMillis != null && expireMillis <= nowMillis,
        supportUrl = info.supportUrl,
        webPageUrl = info.webPageUrl,
    )
}

/**
 * Account information and shortcuts at the start of a subscription group.
 */
@Composable
internal fun SubscriptionInfoCard(
    info: SubscriptionUsageInfo,
    onOpenLink: (String) -> Unit,
    onRefresh: () -> Unit,
    descriptionExpanded: Boolean,
    onDescriptionToggle: () -> Unit,
    onTestLatency: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val model = remember(info) { buildSubscriptionInfoUiModel(info, System.currentTimeMillis()) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier.weight(1f).padding(start = 8.dp)
                        .semantics(mergeDescendants = true) {}
                ) {
                    StatsRow(model, info.hasTrafficInfo)
                    model.trafficFraction?.takeIf { info.hasTrafficInfo }?.let { fraction ->
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.fillMaxWidth().height(4.dp)
                                .clip(RoundedCornerShape(2.dp)).clearAndSetSemantics {},
                            color = if (model.trafficLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
                val links = listOfNotNull(
                    model.webPageUrl?.let { R.string.sub_info_open_page to it },
                    model.supportUrl?.let { R.string.sub_info_open_support to it },
                ).distinctBy { it.second }
                if (links.isNotEmpty()) Box {
                    var showLinks by remember { mutableStateOf(false) }
                    CardIconButton(
                        iconRes = R.drawable.ic_about_24dp,
                        contentDescription = stringResource(R.string.sub_info_open_page),
                        onClick = {
                            if (links.size == 1) onOpenLink(links.single().second) else showLinks = true
                        },
                    )
                    DropdownMenu(expanded = showLinks, onDismissRequest = { showLinks = false }) {
                        links.forEach { (label, url) ->
                            DropdownMenuItem(
                                text = { Text(stringResource(label)) },
                                onClick = { showLinks = false; onOpenLink(url) },
                            )
                        }
                    }
                }
                CardIconButton(
                    iconRes = R.drawable.ic_subscription_speedometer_24dp,
                    contentDescription = stringResource(R.string.title_real_ping_all_server),
                    onClick = onTestLatency,
                )
                CardIconButton(
                    iconRes = R.drawable.ic_refresh_24dp,
                    contentDescription = stringResource(R.string.title_sub_update),
                    onClick = onRefresh,
                )
            }

            info.announcement?.let { announcement ->
                val toggleLabel = stringResource(
                    if (descriptionExpanded) R.string.sub_info_hide_description else R.string.sub_info_show_description
                )
                Row(
                    Modifier.fillMaxWidth().height(28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val actionCount = if (model.webPageUrl != null || model.supportUrl != null) 3 else 2
                    val inactiveEdge = SubscriptionActionWidth * actionCount
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val edge = inactiveEdge.coerceAtMost(((maxWidth - 48.dp) / 2).coerceAtLeast(0.dp))
                        Row(
                            Modifier.fillMaxWidth().height(28.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            HorizontalDivider(Modifier.width(edge))
                            Row(
                                Modifier.weight(1f).height(28.dp).clickable(
                                    role = Role.Button, onClickLabel = toggleLabel, onClick = onDescriptionToggle
                                ).semantics { contentDescription = toggleLabel },
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                HorizontalDivider(Modifier.weight(1f))
                                Icon(
                                    painterResource(R.drawable.ic_expand_more_24dp),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp).rotate(if (descriptionExpanded) 180f else 0f),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                HorizontalDivider(Modifier.weight(1f))
                            }
                            HorizontalDivider(Modifier.width(edge))
                        }
                    }
                }
                AnimatedVisibility(
                    visible = descriptionExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    val paragraphs = remember(announcement) {
                        announcement.trim().split(Regex("\\r?\\n(?:[^\\S\\r\\n]*\\r?\\n)+"))
                    }
                    Column(
                        Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 3.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        paragraphs.forEach { paragraph ->
                            Text(
                                text = paragraph,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    letterSpacing = 0.sp,
                                ),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardIconButton(iconRes: Int, contentDescription: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(SubscriptionActionWidth)) {
        Icon(painterResource(iconRes), contentDescription, Modifier.size(20.dp))
    }
}

@Composable
private fun StatsRow(model: SubscriptionInfoUiModel, hasTrafficInfo: Boolean) {
    val totalText = if (model.totalBytes > 0L) {
        model.totalBytes.toTrafficString()
    } else "∞"
    val traffic = if (hasTrafficInfo) {
        stringResource(R.string.sub_info_traffic, model.usedBytes.toTrafficString(), totalText)
    } else ""
    val expireMillis = model.expireMillis
    val expiry = if (expireMillis != null || hasTrafficInfo) {
        stringResource(R.string.sub_info_expires, subscriptionExpiryLabel(expireMillis))
    } else ""
    val style = MaterialTheme.typography.bodySmall
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = with(density) { maxWidth.roundToPx() }
        val gap = with(density) { 4.dp.roundToPx() }
        val fittedStyle = remember(traffic, expiry, width, gap, style, measurer) {
            var fitted = style
            while (fitted.fontSize.value > 1f) {
                val required = measurer.measure(traffic, fitted, softWrap = false).size.width +
                    measurer.measure(expiry, fitted, softWrap = false).size.width + gap
                if (required <= width) break
                fitted = fitted.copy(fontSize = fitted.fontSize * 0.9f)
            }
            fitted
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (traffic.isNotEmpty()) Text(
                text = traffic,
                style = fittedStyle,
                color = if (model.trafficLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
            )
            Spacer(Modifier.weight(1f))
            if (expiry.isNotEmpty()) Text(
                text = expiry,
                style = fittedStyle,
                color = if (model.expired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

package dev.weft.undercurrent.features.usage

import dev.weft.undercurrent.ui.SectionLabel
import dev.weft.undercurrent.ui.ScreenScaffold
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.weft.harness.cost.UsageTotals
import dev.weft.undercurrent.theme.UndercurrentTheme
import dev.weft.undercurrent.features.usage.UsageViewModel
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate

/**
 * User-facing usage + cost screen. Mirrors [WeftDevTools]' CostTab content
 * but themed for the document aesthetic and reachable from Settings →
 * Usage (not just the debug FAB).
 *
 * Layout: hero row with today's + lifetime totals, a token breakdown,
 * a cache-savings callout, then a by-day bar chart of recent spend.
 *
 * No editable quotas yet — [dev.weft.harness.cost.QuotaPolicy] is set at
 * runtime construction time; making it user-editable would require either
 * a runtime rebuild or a SDK hook to mutate it on the fly. Flagged as a
 * follow-up.
 */
@Composable
internal fun UsageScreen(
    onBack: () -> Unit,
    vm: UsageViewModel = koinViewModel(),
) {
    val totals by vm.totals.collectAsState()
    val today = LocalDate.now().toString()
    val todayUsd = totals.byDay[today] ?: 0.0

    ScreenScaffold(title = "Usage", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item("hero") {
                HeroBlock(todayUsd = todayUsd, lifetimeUsd = totals.lifetimeUsd)
            }
            item("tokens") {
                SectionLabel(text = "Tokens", modifier = Modifier.padding(start = 0.dp, top = 0.dp, bottom = 0.dp))
                Spacer(Modifier.height(8.dp))
                TokenBreakdown(totals = totals)
            }
            if (totals.lifetimeCacheReadTokens > 0) {
                item("cache") {
                    SectionLabel(text = "Cache savings", modifier = Modifier.padding(start = 0.dp, top = 0.dp, bottom = 0.dp))
                    Spacer(Modifier.height(8.dp))
                    CacheSavingsBlock(totals = totals)
                }
            }
            if (totals.byDay.isNotEmpty()) {
                item("history-label") {
                    SectionLabel(text = "Last 14 days", modifier = Modifier.padding(start = 0.dp, top = 0.dp, bottom = 0.dp))
                }
                item("chart") { ByDayChart(byDay = totals.byDay) }
            }
            if (totals.lastCallModelId != null) {
                item("last-model") {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Last model: ${totals.lastCallModelId}",
                        style = UndercurrentTheme.typography.sansSmall.copy(
                            color = UndercurrentTheme.colors.inkSubtle,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroBlock(todayUsd: Double, lifetimeUsd: Double) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        StatColumn(
            modifier = Modifier.weight(1f),
            label = "Today",
            value = "$%.3f".format(todayUsd),
            valueColor = colors.accent,
        )
        StatColumn(
            modifier = Modifier.weight(1f),
            label = "Lifetime",
            value = "$%.2f".format(lifetimeUsd),
            valueColor = colors.ink,
        )
    }
}

@Composable
private fun StatColumn(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = typography.sansLabel.copy(color = colors.inkSubtle),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = typography.sansHeader.copy(
                color = valueColor,
                fontSize = typography.sansHeader.fontSize * 2.2f,
            ),
        )
    }
}

@Composable
private fun TokenBreakdown(totals: UsageTotals) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .background(colors.surfaceMuted)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TokenRow(label = "Input", value = formatTokens(totals.lifetimeInputTokens))
        TokenRow(label = "Output", value = formatTokens(totals.lifetimeOutputTokens))
        if (totals.lifetimeCacheReadTokens > 0) {
            TokenRow(label = "Cache reads", value = formatTokens(totals.lifetimeCacheReadTokens))
        }
        if (totals.lifetimeCacheWriteTokens > 0) {
            TokenRow(label = "Cache writes", value = formatTokens(totals.lifetimeCacheWriteTokens))
        }
    }
}

@Composable
private fun TokenRow(label: String, value: String) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = typography.sansSmall.copy(color = colors.inkMuted),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = typography.mono.copy(color = colors.ink),
        )
    }
}

@Composable
private fun CacheSavingsBlock(totals: UsageTotals) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    // Anthropic charges cache reads at ~10% of base input rate. The savings
    // estimate below is "input tokens we would have paid for at base rate
    // minus what we actually paid via cache" — a directional figure, not
    // a precise dollar amount (rates differ across models).
    val savedFraction = 0.9f // 90% off base rate for cache reads
    val approxBaseInputPriceUsdPerMillion = 3.0 // Sonnet 4.6 input rate, USD per 1M tokens
    val savedUsd = (totals.lifetimeCacheReadTokens / 1_000_000.0) *
        approxBaseInputPriceUsdPerMillion * savedFraction

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .background(colors.surfaceMuted)
            .padding(16.dp),
    ) {
        Text(
            text = "~$%.2f saved".format(savedUsd),
            style = typography.sansHeader.copy(
                color = colors.accent,
                fontSize = typography.sansHeader.fontSize * 1.4f,
            ),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${formatTokens(totals.lifetimeCacheReadTokens)} cache-read tokens, " +
                "billed at ~10% of base input rate.",
            style = typography.sansSmall.copy(color = colors.inkMuted),
        )
    }
}

@Composable
private fun ByDayChart(byDay: Map<String, Double>) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    // Take the last 14 days, oldest-first for left-to-right reading.
    val entries = remember(byDay) {
        byDay.toSortedMap()  // ISO date strings sort lexicographically = chronologically
            .entries
            .toList()
            .takeLast(14)
    }
    val maxUsd = entries.maxOfOrNull { it.value }?.coerceAtLeast(0.0001) ?: 0.0001

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            entries.forEach { (_, usd) ->
                val fraction = (usd / maxUsd).coerceAtLeast(0.02).toFloat()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(fraction)
                        .clip(shapes.xsmall)
                        .background(colors.accent),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            entries.firstOrNull()?.let { (date, _) ->
                Text(
                    text = date,
                    style = typography.sansSmall.copy(color = colors.inkSubtle),
                )
            }
            entries.lastOrNull()?.let { (date, _) ->
                Text(
                    text = date,
                    style = typography.sansSmall.copy(color = colors.inkSubtle),
                )
            }
        }
    }
}

private fun formatTokens(n: Int): String = when {
    n >= 1_000_000 -> "%.2fM".format(n / 1_000_000.0)
    n >= 1_000 -> "%.1fk".format(n / 1_000.0)
    else -> n.toString()
}

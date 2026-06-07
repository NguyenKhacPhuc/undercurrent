package dev.weft.undercurrent.feature.settings.usage

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.core.resources.Res
import dev.weft.undercurrent.core.resources.usage_cache_savings_body_format
import dev.weft.undercurrent.core.resources.usage_cache_savings_header_format
import dev.weft.undercurrent.core.resources.usage_cache_savings_label
import dev.weft.undercurrent.core.resources.usage_history_label
import dev.weft.undercurrent.core.resources.usage_label_cache_reads
import dev.weft.undercurrent.core.resources.usage_label_cache_writes
import dev.weft.undercurrent.core.resources.usage_label_input
import dev.weft.undercurrent.core.resources.usage_label_lifetime
import dev.weft.undercurrent.core.resources.usage_label_output
import dev.weft.undercurrent.core.resources.usage_label_today
import dev.weft.undercurrent.core.resources.usage_last_model_format
import dev.weft.undercurrent.core.resources.usage_title
import dev.weft.undercurrent.core.resources.usage_tokens_label
import dev.weft.undercurrent.core.ui.ScreenScaffold
import dev.weft.undercurrent.core.ui.SectionLabel
import dev.weft.undercurrent.core.domain.UsageTotals
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * User-facing usage + cost screen. Reachable from Settings → Usage.
 * Read-only: hero totals, token breakdown, cache-savings callout, and a
 * by-day spend chart. Stateless — [UsageRoute] hoists [UsageState].
 */
@OptIn(ExperimentalTime::class)
@Composable
fun UsageScreen(
    state: UsageState,
    onBack: () -> Unit,
) {
    val totals = state.totals
    val today = remember {
        Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString()
    }
    val todayUsd = totals.byDay[today] ?: 0.0

    ScreenScaffold(title = stringResource(Res.string.usage_title), onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item("hero") {
                HeroBlock(todayUsd = todayUsd, lifetimeUsd = totals.lifetimeUsd)
            }
            item("tokens") {
                SectionLabel(text = stringResource(Res.string.usage_tokens_label), modifier = Modifier.padding(start = 0.dp, top = 0.dp, bottom = 0.dp))
                Spacer(Modifier.height(8.dp))
                TokenBreakdown(totals = totals)
            }
            if (totals.lifetimeCacheReadTokens > 0) {
                item("cache") {
                    SectionLabel(text = stringResource(Res.string.usage_cache_savings_label), modifier = Modifier.padding(start = 0.dp, top = 0.dp, bottom = 0.dp))
                    Spacer(Modifier.height(8.dp))
                    CacheSavingsBlock(totals = totals)
                }
            }
            if (totals.byDay.isNotEmpty()) {
                item("history-label") {
                    SectionLabel(text = stringResource(Res.string.usage_history_label), modifier = Modifier.padding(start = 0.dp, top = 0.dp, bottom = 0.dp))
                }
                item("chart") { ByDayChart(byDay = totals.byDay) }
            }
            if (totals.lastCallModelId != null) {
                item("last-model") {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(Res.string.usage_last_model_format, totals.lastCallModelId!!),
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        StatColumn(
            modifier = Modifier.weight(1f),
            label = stringResource(Res.string.usage_label_today),
            value = "$${formatDecimal(todayUsd, 3)}",
            valueColor = colors.accent,
        )
        StatColumn(
            modifier = Modifier.weight(1f),
            label = stringResource(Res.string.usage_label_lifetime),
            value = "$${formatDecimal(lifetimeUsd, 2)}",
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
    val shapes = UndercurrentTheme.shapes
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .background(colors.surfaceMuted)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TokenRow(label = stringResource(Res.string.usage_label_input), value = formatTokens(totals.lifetimeInputTokens))
        TokenRow(label = stringResource(Res.string.usage_label_output), value = formatTokens(totals.lifetimeOutputTokens))
        if (totals.lifetimeCacheReadTokens > 0) {
            TokenRow(label = stringResource(Res.string.usage_label_cache_reads), value = formatTokens(totals.lifetimeCacheReadTokens))
        }
        if (totals.lifetimeCacheWriteTokens > 0) {
            TokenRow(label = stringResource(Res.string.usage_label_cache_writes), value = formatTokens(totals.lifetimeCacheWriteTokens))
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
    // Anthropic charges cache reads at ~10% of base input rate. The
    // savings estimate is "input tokens we would have paid for at base
    // rate minus what we actually paid via cache" — directional, not
    // a precise dollar amount (rates differ across models).
    val savedFraction = 0.9
    val approxBaseInputPriceUsdPerMillion = 3.0 // Sonnet 4.6 input rate
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
            text = stringResource(Res.string.usage_cache_savings_header_format, formatDecimal(savedUsd, 2)),
            style = typography.sansHeader.copy(
                color = colors.accent,
                fontSize = typography.sansHeader.fontSize * 1.4f,
            ),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.usage_cache_savings_body_format, formatTokens(totals.lifetimeCacheReadTokens)),
            style = typography.sansSmall.copy(color = colors.inkMuted),
        )
    }
}

@Composable
private fun ByDayChart(byDay: Map<String, Double>) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    // ISO date strings sort lexicographically = chronologically.
    val entries = remember(byDay) {
        byDay.entries.sortedBy { it.key }.takeLast(14)
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
    n >= 1_000_000 -> "${formatDecimal(n / 1_000_000.0, 2)}M"
    n >= 1_000 -> "${formatDecimal(n / 1_000.0, 1)}k"
    else -> n.toString()
}

@Preview
@Composable
private fun UsageScreenPreview() {
    UndercurrentTheme {
        UsageScreen(
            state = UsageState(
                totals = UsageTotals(
                    lifetimeUsd = 4.27,
                    lifetimeInputTokens = 124_580,
                    lifetimeOutputTokens = 38_910,
                    lifetimeCacheReadTokens = 92_400,
                    lifetimeCacheWriteTokens = 12_300,
                    byDay = mapOf(
                        "2026-05-24" to 0.45,
                        "2026-05-25" to 0.32,
                        "2026-05-26" to 0.18,
                        "2026-05-27" to 0.71,
                        "2026-05-28" to 0.22,
                        "2026-05-29" to 0.91,
                        "2026-05-30" to 0.14,
                    ),
                    lastCallModelId = "claude-sonnet-4-5",
                ),
            ),
            onBack = {},
        )
    }
}

/**
 * commonMain replacement for `"%.${decimals}f".format(value)`. Banker's
 * rounding isn't necessary here — half-up is what `%.f` does on JVM.
 */
private fun formatDecimal(value: Double, decimals: Int): String {
    require(decimals in 0..9) { "decimals out of range: $decimals" }
    val mult = 10.0.pow(decimals)
    val scaled = (value * mult).roundToLong()
    val whole = scaled / mult.toLong()
    if (decimals == 0) return whole.toString()
    val frac = scaled.absoluteValue % mult.toLong()
    val sign = if (value < 0 && whole == 0L) "-" else ""
    return "$sign$whole.${frac.toString().padStart(decimals, '0')}"
}

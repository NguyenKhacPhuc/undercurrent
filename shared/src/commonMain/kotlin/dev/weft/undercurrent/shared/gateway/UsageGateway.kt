package dev.weft.undercurrent.shared.gateway

import kotlinx.coroutines.flow.StateFlow

/**
 * Read-only cost / quota totals for the usage screen. Backed by Weft's
 * `UsageStore` on Android; iOS stub holds the zero-valued totals.
 *
 * Quota policy editing (Weft's `QuotaPolicy`) isn't exposed yet — that
 * lands when the usage screen gains write controls.
 */
public interface UsageGateway {

    /** Live snapshot of accumulated usage. */
    public val totals: StateFlow<UsageTotals>
}

/** Mirror of `dev.weft.harness.cost.UsageTotals`. */
public data class UsageTotals(
    val lifetimeUsd: Double = 0.0,
    val lifetimeInputTokens: Int = 0,
    val lifetimeOutputTokens: Int = 0,
    val lifetimeCacheReadTokens: Int = 0,
    val lifetimeCacheWriteTokens: Int = 0,
    val byDay: Map<String, Double> = emptyMap(),
    val byAgent: Map<String, Double> = emptyMap(),
    val lastCallUsd: Double = 0.0,
    val lastCallTokens: Int = 0,
    val lastCallModelId: String? = null,
    val lastCallCacheReadTokens: Int = 0,
    val lastCallCacheWriteTokens: Int = 0,
)

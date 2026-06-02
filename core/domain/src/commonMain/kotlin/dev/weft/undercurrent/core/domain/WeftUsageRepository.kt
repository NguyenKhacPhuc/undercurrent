package dev.weft.undercurrent.core.domain

import dev.weft.harness.cost.UsageStore
import dev.weft.undercurrent.core.domain.UsageRepository
import dev.weft.undercurrent.core.domain.UsageTotals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import dev.weft.harness.cost.UsageTotals as WeftUsageTotals

/**
 * Android impl of [UsageRepository] backed by Weft's [UsageStore].
 */
class WeftUsageRepository(
    private val store: UsageStore,
) : UsageRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val totals: StateFlow<UsageTotals> = store.totals
        .map { it.toCommon() }
        .stateIn(scope, SharingStarted.Eagerly, UsageTotals())

    private fun WeftUsageTotals.toCommon(): UsageTotals = UsageTotals(
        lifetimeUsd = lifetimeUsd,
        lifetimeInputTokens = lifetimeInputTokens,
        lifetimeOutputTokens = lifetimeOutputTokens,
        lifetimeCacheReadTokens = lifetimeCacheReadTokens,
        lifetimeCacheWriteTokens = lifetimeCacheWriteTokens,
        byDay = byDay,
        byAgent = byAgent,
        lastCallUsd = lastCallUsd,
        lastCallTokens = lastCallTokens,
        lastCallModelId = lastCallModelId,
        lastCallCacheReadTokens = lastCallCacheReadTokens,
        lastCallCacheWriteTokens = lastCallCacheWriteTokens,
    )
}

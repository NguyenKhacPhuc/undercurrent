package dev.weft.undercurrent.data.weft

import dev.weft.harness.cost.UsageStore
import dev.weft.undercurrent.shared.gateway.UsageGateway
import dev.weft.undercurrent.shared.gateway.UsageTotals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import dev.weft.harness.cost.UsageTotals as WeftUsageTotals

/**
 * Android impl of [UsageGateway] backed by Weft's [UsageStore].
 */
public class WeftUsageGateway(
    private val store: UsageStore,
) : UsageGateway {

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

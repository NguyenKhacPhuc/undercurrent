package dev.weft.undercurrent.core.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS stub. With no agent running, lifetime totals are flat zero.
 */
class StubUsageRepository : UsageRepository {
    override val totals: StateFlow<UsageTotals> =
        MutableStateFlow(UsageTotals()).asStateFlow()
}

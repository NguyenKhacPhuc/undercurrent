package dev.weft.undercurrent.shared.gateway

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS stub. With no agent running, lifetime totals are flat zero.
 */
public class StubUsageGateway : UsageGateway {
    override val totals: StateFlow<UsageTotals> =
        MutableStateFlow(UsageTotals()).asStateFlow()
}

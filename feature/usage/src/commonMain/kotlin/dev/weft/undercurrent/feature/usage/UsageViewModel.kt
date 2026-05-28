package dev.weft.undercurrent.feature.usage

import androidx.lifecycle.ViewModel
import dev.weft.undercurrent.shared.gateway.UsageGateway
import dev.weft.undercurrent.shared.gateway.UsageTotals
import kotlinx.coroutines.flow.StateFlow

/**
 * Screen-scoped wrapper around the runtime's [UsageTotals] flow. There
 * are no actions on this screen — it's read-only — so this VM is just
 * a typed pull-through so [UsageScreen] can resolve via `koinViewModel()`
 * instead of receiving the gateway from prop-drilling.
 *
 * If/when quota editing lands (Weft's `QuotaPolicy`) the relevant
 * suspend handlers belong here.
 *
 * KMP — commonMain. Moved from `app/.../features/usage/UsageViewModel.kt`.
 * Now consumes [UsageGateway] (was `WeftRuntime.usageStore` directly).
 */
public class UsageViewModel(
    gateway: UsageGateway,
) : ViewModel() {
    public val totals: StateFlow<UsageTotals> = gateway.totals
}

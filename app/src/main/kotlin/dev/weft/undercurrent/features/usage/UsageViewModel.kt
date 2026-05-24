package dev.weft.undercurrent.features.usage

import androidx.lifecycle.ViewModel
import dev.weft.android.WeftRuntime
import dev.weft.harness.cost.UsageTotals
import kotlinx.coroutines.flow.StateFlow

/**
 * Screen-scoped wrapper around the runtime's [UsageTotals] flow. There are
 * no actions on this screen — it's read-only — so this VM is just a typed
 * pull-through so [dev.weft.undercurrent.features.usage.UsageScreen] can resolve via
 * `koinViewModel()` instead of receiving `runtime.usageStore` from
 * MainActivity prop-drilling.
 *
 * If/when quota editing lands ([dev.weft.harness.cost.QuotaPolicy]) the
 * relevant suspend handlers belong here.
 */
internal class UsageViewModel(
    runtime: WeftRuntime,
) : ViewModel() {
    val totals: StateFlow<UsageTotals> = runtime.usageStore.totals
}

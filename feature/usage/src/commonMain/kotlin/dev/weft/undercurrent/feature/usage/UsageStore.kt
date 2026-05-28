package dev.weft.undercurrent.feature.usage

import androidx.lifecycle.viewModelScope
import dev.weft.undercurrent.shared.gateway.UsageGateway
import dev.weft.undercurrent.shared.gateway.UsageTotals
import dev.weft.undercurrent.shared.mvi.Store
import kotlinx.coroutines.launch

/**
 * Read-only screen — no actions today. The MVI shape is kept for
 * consistency across features (and future quota-editing work fits
 * cleanly: add `SetQuota(usd)` to [UsageIntent], handle it here).
 */
public data class UsageState(public val totals: UsageTotals = UsageTotals())

public sealed interface UsageIntent
public sealed interface UsageEffect

public class UsageStore(
    gateway: UsageGateway,
) : Store<UsageState, UsageIntent, UsageEffect>(
    initialState = UsageState(totals = gateway.totals.value),
) {
    init {
        viewModelScope.launch {
            gateway.totals.collect { t -> update { it.copy(totals = t) } }
        }
    }

    override fun dispatch(intent: UsageIntent) {
        // No-op — no intents defined yet.
    }
}

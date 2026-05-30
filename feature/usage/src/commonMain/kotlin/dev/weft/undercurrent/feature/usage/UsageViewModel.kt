package dev.weft.undercurrent.feature.usage

import dev.weft.undercurrent.core.domain.UsageRepository
import dev.weft.undercurrent.core.domain.UsageTotals
import dev.weft.undercurrent.shared.mvi.MviViewModel

data class UsageState(val totals: UsageTotals = UsageTotals())

sealed interface UsageIntent
sealed interface UsageEffect

class UsageViewModel(
    gateway: UsageRepository,
) : MviViewModel<UsageState, UsageIntent, UsageEffect>(
    initialState = UsageState(totals = gateway.totals.value),
) {
    init {
        gateway.totals.collectInto { copy(totals = it) }
    }

    override fun dispatch(intent: UsageIntent) = launch { }
}

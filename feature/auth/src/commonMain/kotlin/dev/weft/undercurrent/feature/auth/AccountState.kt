package dev.weft.undercurrent.feature.auth

import dev.weft.undercurrent.core.domain.auth.dto.AccountDto

data class AccountState(val view: View = View.Loading) {
    sealed interface View {
        data object Loading : View
        data class Loaded(val account: AccountDto) : View
        data object LoadError : View
    }
}

sealed interface AccountIntent {
    data object Refresh : AccountIntent
    data object SignOut : AccountIntent
}

sealed interface AccountEffect {
    data object SignedOut : AccountEffect
}

package dev.weft.undercurrent.feature.auth

import dev.weft.undercurrent.core.domain.AuthRepository
import dev.weft.undercurrent.core.domain.SessionTokenStore
import dev.weft.undercurrent.core.ext.Result
import dev.weft.undercurrent.data.network.common.ApiException
import dev.weft.undercurrent.data.network.common.HttpStatus
import dev.weft.undercurrent.shared.mvi.MviViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

class AccountViewModel(
    private val authRepository: AuthRepository,
    private val sessionTokenStore: SessionTokenStore,
) : MviViewModel<AccountState, AccountIntent, AccountEffect>(AccountState()) {

    init {
        refresh()
    }

    override fun dispatch(intent: AccountIntent) = launch {
        when (intent) {
            AccountIntent.Refresh -> doRefresh()
            AccountIntent.SignOut -> doSignOut()
        }
    }

    private fun refresh() = launch { doRefresh() }

    private suspend fun doRefresh() {
        update { it.copy(view = AccountState.View.Loading) }
        authRepository.getMe()
            .onEach { result ->
                when (result) {
                    Result.Loading -> Unit
                    is Result.Success ->
                        update { it.copy(view = AccountState.View.Loaded(result.data.account)) }
                    is Result.Error -> handleGetMeError(result.exception)
                }
            }
            .collect()
    }

    private suspend fun handleGetMeError(e: Throwable) {
        val api = e as? ApiException
        if (api?.httpStatus == HttpStatus.UNAUTHORIZED) {
            sessionTokenStore.clear()
            emit(AccountEffect.SignedOut)
        } else {
            update { it.copy(view = AccountState.View.LoadError) }
        }
    }

    private suspend fun doSignOut() {
        authRepository.signOut().collect()
        sessionTokenStore.clear()
        emit(AccountEffect.SignedOut)
    }
}

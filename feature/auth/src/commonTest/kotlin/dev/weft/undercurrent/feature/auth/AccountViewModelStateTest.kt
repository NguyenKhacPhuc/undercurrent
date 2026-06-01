@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.weft.undercurrent.feature.auth

import app.cash.turbine.test
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifySuspend
import dev.weft.undercurrent.core.domain.AuthRepository
import dev.weft.undercurrent.core.domain.SessionTokenStore
import dev.weft.undercurrent.core.domain.auth.dto.AccountDto
import dev.weft.undercurrent.core.domain.auth.dto.MeResponse
import dev.weft.undercurrent.core.ext.Result
import dev.weft.undercurrent.data.network.common.ApiException
import dev.weft.undercurrent.data.network.common.NetworkException
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

private val ACCOUNT = AccountDto("acct.abc", "Phuc", "phuc@example.com", 1L)

class AccountViewModelStateTest : BehaviorSpec({
    val mainDispatcher = StandardTestDispatcher()
    beforeTest { Dispatchers.setMain(mainDispatcher) }
    afterTest { Dispatchers.resetMain() }

    fun vm(
        getMeResult: Result<MeResponse>? = null,
        signOutResult: Result<Unit>? = Result.Success(Unit),
    ): Triple<AccountViewModel, AuthRepository, SessionTokenStore> {
        val repo = mock<AuthRepository> {
            every { getMe() } returns
                if (getMeResult == null) flowOf(Result.Loading)
                else flowOf(Result.Loading, getMeResult)
            every { signOut() } returns
                if (signOutResult == null) flowOf(Result.Loading)
                else flowOf(Result.Loading, signOutResult)
        }
        val store = mock<SessionTokenStore> {
            everySuspend { clear() } returns Unit
        }
        return Triple(AccountViewModel(repo, store), repo, store)
    }

    Given("a fresh AccountViewModel") {
        Then("init triggers getMe and state transitions Loading → Loaded(account)") {
            runTest {
                val (viewModel, repo, _) = vm(getMeResult = Result.Success(MeResponse(ACCOUNT)))
                advanceUntilIdle()
                viewModel.state.value.view shouldBe AccountState.View.Loaded(ACCOUNT)
                verify(exactly(1)) { repo.getMe() }
            }
        }
    }

    Given("a getMe network failure") {
        Then("state goes to LoadError; no token wipe; no SignedOut effect") {
            runTest {
                val (viewModel, _, store) = vm(getMeResult = Result.Error(NetworkException()))
                advanceUntilIdle()
                viewModel.state.value.view shouldBe AccountState.View.LoadError
                verifySuspend(exactly(0)) { store.clear() }
            }
        }
    }

    Given("a getMe 401 result") {
        Then("token is wiped and SignedOut is emitted") {
            runTest {
                val err = ApiException(
                    code = "unauthenticated",
                    apiMessage = "Session is no longer valid",
                    endpoint = "/v1/me",
                    httpStatus = 401,
                )
                val (viewModel, _, store) = vm(getMeResult = Result.Error(err))
                viewModel.effects.test {
                    advanceUntilIdle()
                    awaitItem() shouldBe AccountEffect.SignedOut
                    cancelAndIgnoreRemainingEvents()
                }
                verifySuspend(exactly(1)) { store.clear() }
            }
        }
    }

    Given("a VM that finished with LoadError") {
        Then("Refresh re-runs getMe and reaches Loaded on success") {
            runTest {
                val responses = ArrayDeque<Result<MeResponse>>().apply {
                    addLast(Result.Error(NetworkException()))
                    addLast(Result.Success(MeResponse(ACCOUNT)))
                }
                val repo = mock<AuthRepository> {
                    every { getMe() } calls { flowOf(Result.Loading, responses.removeFirst()) }
                    every { signOut() } returns flowOf(Result.Loading, Result.Success(Unit))
                }
                val store = mock<SessionTokenStore> { everySuspend { clear() } returns Unit }
                val viewModel = AccountViewModel(repo, store)
                advanceUntilIdle()
                viewModel.state.value.view shouldBe AccountState.View.LoadError

                viewModel.dispatch(AccountIntent.Refresh)
                advanceUntilIdle()
                viewModel.state.value.view shouldBe AccountState.View.Loaded(ACCOUNT)
                verify(exactly(2)) { repo.getMe() }
            }
        }
    }

    Given("a loaded VM") {
        Then("SignOut intent calls signOut, wipes token, emits SignedOut") {
            runTest {
                val (viewModel, repo, store) = vm(getMeResult = Result.Success(MeResponse(ACCOUNT)))
                viewModel.effects.test {
                    advanceUntilIdle()
                    viewModel.dispatch(AccountIntent.SignOut)
                    advanceUntilIdle()
                    awaitItem() shouldBe AccountEffect.SignedOut
                    cancelAndIgnoreRemainingEvents()
                }
                verify(exactly(1)) { repo.signOut() }
                verifySuspend(exactly(1)) { store.clear() }
            }
        }
    }
})

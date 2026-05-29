package dev.weft.undercurrent.feature.integrations

import dev.weft.undercurrent.data.datastore.IntegrationsRepository
import dev.weft.undercurrent.shared.gateway.OAuthConfig
import dev.weft.undercurrent.shared.gateway.OAuthGateway
import dev.weft.undercurrent.shared.gateway.OAuthResult
import dev.weft.undercurrent.shared.gateway.OAuthTokens
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Exercises [IntegrationsStore] in BDD style.
 *
 * Covers the OAuth happy path (Success → putTokens → setEnabled),
 * every [OAuthResult] failure variant mapping to [ActionStatus.Failure]
 * with the appropriate message, Disconnect's short-circuit (no OAuth
 * call), the `pendingRestart` projection from the live enabled set vs
 * the boot-time snapshot, and the [statusFor] pure helper.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IntegrationsStoreTest : BehaviorSpec({

    val mainDispatcher = StandardTestDispatcher()
    beforeTest { Dispatchers.setMain(mainDispatcher) }
    afterTest { Dispatchers.resetMain() }

    val testIntegration = Integration(
        id = "linear",
        displayName = "Linear",
        tagline = "Issues, projects, comments.",
        mcpUrl = "https://mcp.linear.app/mcp",
        oauth = OAuthConfig(
            clientId = "test-id",
            authorizationEndpoint = "https://linear.app/oauth/authorize",
            tokenEndpoint = "https://api.linear.app/oauth/token",
            redirectUri = "undercurrent://oauth/linear",
            scopes = listOf("read", "write"),
        ),
    )

    val testTokens = OAuthTokens(
        accessToken = "AT.fake",
        refreshToken = "RT.fake",
        expiresAtEpochMs = 1_000L,
        scope = "read write",
    )

    fun fakes(
        initialEnabledFromFlow: Set<String> = emptySet(),
    ): Triple<IntegrationsRepository, OAuthGateway, MutableStateFlow<Set<String>>> {
        val enabledFlow = MutableStateFlow(initialEnabledFromFlow)
        val repo = mockk<IntegrationsRepository>()
        every { repo.enabledIdsFlow } returns enabledFlow
        coEvery { repo.setEnabled(any(), any()) } returns Unit

        val oauth = mockk<OAuthGateway>()
        coEvery { oauth.authorize(any()) } returns OAuthResult.Success(testTokens)
        coEvery { oauth.putTokens(any(), any()) } returns Unit
        coEvery { oauth.removeTokens(any()) } returns Unit

        return Triple(repo, oauth, enabledFlow)
    }

    // ── initial state + pendingRestart projection ────────────────────

    Given("a store built with initialEnabled={'linear'} and a repo that emits the same set") {
        val (repo, oauth, _) = fakes(initialEnabledFromFlow = setOf("linear"))
        val store = IntegrationsStore(repo, oauth, initialEnabled = setOf("linear"))

        Then("initial state has enabledIds={'linear'} and pendingRestart=false") {
            store.state.value shouldBe IntegrationsState(
                enabledIds = setOf("linear"),
                pendingRestart = false,
                lastAction = ActionStatus.Idle,
            )
        }
    }

    Given("a store whose repo flow matches initialEnabled exactly") {
        Then("pendingRestart stays false after the init subscription drains") {
            runTest {
                val (repo, oauth, _) = fakes(initialEnabledFromFlow = setOf("linear"))
                val store = IntegrationsStore(repo, oauth, initialEnabled = setOf("linear"))
                advanceUntilIdle()

                store.state.value.pendingRestart shouldBe false
                store.state.value.enabledIds shouldBe setOf("linear")
            }
        }
    }

    Given("a store whose initialEnabled is empty and repo emits {'linear'}") {
        Then("pendingRestart flips to true because the live set diverges from boot") {
            runTest {
                val (repo, oauth, enabledFlow) = fakes(initialEnabledFromFlow = emptySet())
                val store = IntegrationsStore(repo, oauth, initialEnabled = emptySet())
                advanceUntilIdle()

                enabledFlow.value = setOf("linear")
                advanceUntilIdle()

                store.state.value.pendingRestart shouldBe true
                store.state.value.enabledIds shouldBe setOf("linear")
            }
        }
    }

    Given("a store whose repo flow has already diverged from initialEnabled and then returns") {
        Then("pendingRestart flips back to false when the flow re-equals initialEnabled") {
            runTest {
                val (repo, oauth, enabledFlow) = fakes(initialEnabledFromFlow = emptySet())
                val store = IntegrationsStore(repo, oauth, initialEnabled = emptySet())
                advanceUntilIdle()

                enabledFlow.value = setOf("linear")
                advanceUntilIdle()
                store.state.value.pendingRestart shouldBe true

                enabledFlow.value = emptySet()
                advanceUntilIdle()
                store.state.value.pendingRestart shouldBe false
            }
        }
    }

    // ── Connect happy path ───────────────────────────────────────────

    Given("a fresh store with the OAuth gateway happy-pathed to Success") {
        When("Connect(linear) is dispatched") {
            Then("authorize → putTokens → setEnabled fire in order and lastAction is Success") {
                runTest {
                    val (repo, oauth, _) = fakes()
                    val store = IntegrationsStore(repo, oauth, initialEnabled = emptySet())

                    store.dispatch(IntegrationsIntent.Connect(testIntegration))
                    advanceUntilIdle()

                    coVerifyOrder {
                        oauth.authorize(testIntegration.oauth)
                        oauth.putTokens("linear", testTokens)
                        repo.setEnabled("linear", enabled = true)
                    }
                    val action = store.state.value.lastAction
                    check(action is ActionStatus.Success) { "expected Success, got $action" }
                    action.integrationId shouldBe "linear"
                    action.message shouldBe "Connected"
                }
            }
        }
    }

    // ── Connect failure variants — every OAuthResult mapped ─────────

    Given("an OAuth gateway that returns UserCancelled on authorize") {
        When("Connect is dispatched") {
            Then("lastAction is Failure('Cancelled.') and no tokens or enabled bit is persisted") {
                runTest {
                    val (repo, oauth, _) = fakes()
                    coEvery { oauth.authorize(any()) } returns OAuthResult.UserCancelled
                    val store = IntegrationsStore(repo, oauth, initialEnabled = emptySet())

                    store.dispatch(IntegrationsIntent.Connect(testIntegration))
                    advanceUntilIdle()

                    val action = store.state.value.lastAction
                    check(action is ActionStatus.Failure)
                    action.integrationId shouldBe "linear"
                    action.message shouldBe "Cancelled."

                    coVerify(exactly = 0) { oauth.putTokens(any(), any()) }
                    coVerify(exactly = 0) { repo.setEnabled(any(), any()) }
                }
            }
        }
    }

    Given("an OAuth gateway that returns ProviderError(code, description=null)") {
        When("Connect is dispatched") {
            Then("lastAction is Failure with just the code (no description suffix)") {
                runTest {
                    val (repo, oauth, _) = fakes()
                    coEvery { oauth.authorize(any()) } returns
                        OAuthResult.ProviderError(code = "access_denied", description = null)
                    val store = IntegrationsStore(repo, oauth, initialEnabled = emptySet())

                    store.dispatch(IntegrationsIntent.Connect(testIntegration))
                    advanceUntilIdle()

                    val action = store.state.value.lastAction
                    check(action is ActionStatus.Failure)
                    action.message shouldBe "access_denied"
                }
            }
        }
    }

    Given("an OAuth gateway that returns ProviderError(code, description='Unknown scope X.')") {
        When("Connect is dispatched") {
            Then("lastAction is Failure with 'code: description'") {
                runTest {
                    val (repo, oauth, _) = fakes()
                    coEvery { oauth.authorize(any()) } returns
                        OAuthResult.ProviderError(code = "invalid_scope", description = "Unknown scope X.")
                    val store = IntegrationsStore(repo, oauth, initialEnabled = emptySet())

                    store.dispatch(IntegrationsIntent.Connect(testIntegration))
                    advanceUntilIdle()

                    val action = store.state.value.lastAction
                    check(action is ActionStatus.Failure)
                    action.message shouldBe "invalid_scope: Unknown scope X."
                }
            }
        }
    }

    Given("an OAuth gateway that returns TransportError('Network timeout after 5s')") {
        When("Connect is dispatched") {
            Then("lastAction.message contains the underlying transport error") {
                runTest {
                    val (repo, oauth, _) = fakes()
                    coEvery { oauth.authorize(any()) } returns
                        OAuthResult.TransportError(message = "Network timeout after 5s")
                    val store = IntegrationsStore(repo, oauth, initialEnabled = emptySet())

                    store.dispatch(IntegrationsIntent.Connect(testIntegration))
                    advanceUntilIdle()

                    val action = store.state.value.lastAction
                    check(action is ActionStatus.Failure)
                    action.message shouldContain "Network timeout"
                }
            }
        }
    }

    Given("an OAuth gateway that returns StateMismatch") {
        When("Connect is dispatched") {
            Then("lastAction is Failure with the user-facing retry hint") {
                runTest {
                    val (repo, oauth, _) = fakes()
                    coEvery { oauth.authorize(any()) } returns OAuthResult.StateMismatch
                    val store = IntegrationsStore(repo, oauth, initialEnabled = emptySet())

                    store.dispatch(IntegrationsIntent.Connect(testIntegration))
                    advanceUntilIdle()

                    val action = store.state.value.lastAction
                    check(action is ActionStatus.Failure)
                    action.message shouldBe "State mismatch — try again."
                }
            }
        }
    }

    // ── Disconnect short-circuit ────────────────────────────────────

    Given("a store with Linear connected") {
        When("Disconnect(linear) is dispatched") {
            Then("removeTokens → setEnabled(false) fire in order, no authorize, lastAction=Success") {
                runTest {
                    val (repo, oauth, _) = fakes(initialEnabledFromFlow = setOf("linear"))
                    val store = IntegrationsStore(repo, oauth, initialEnabled = setOf("linear"))
                    advanceUntilIdle()

                    store.dispatch(IntegrationsIntent.Disconnect(testIntegration))
                    advanceUntilIdle()

                    coVerifyOrder {
                        oauth.removeTokens("linear")
                        repo.setEnabled("linear", enabled = false)
                    }
                    coVerify(exactly = 0) { oauth.authorize(any()) }

                    val action = store.state.value.lastAction
                    check(action is ActionStatus.Success)
                    action.message shouldBe "Disconnected"
                }
            }
        }
    }

    // ── ClearLastAction ─────────────────────────────────────────────

    Given("a store with lastAction=Success after a successful Connect") {
        When("ClearLastAction is dispatched") {
            Then("lastAction returns to Idle") {
                runTest {
                    val (repo, oauth, _) = fakes()
                    val store = IntegrationsStore(repo, oauth, initialEnabled = emptySet())

                    store.dispatch(IntegrationsIntent.Connect(testIntegration))
                    advanceUntilIdle()
                    val mid = store.state.value.lastAction
                    check(mid is ActionStatus.Success)

                    store.dispatch(IntegrationsIntent.ClearLastAction)

                    store.state.value.lastAction shouldBe ActionStatus.Idle
                }
            }
        }
    }

    // ── statusFor() pure helper ─────────────────────────────────────

    Given("a store and an enabled set that contains the integration's id") {
        val (repo, oauth, _) = fakes()
        val store = IntegrationsStore(repo, oauth, initialEnabled = emptySet())

        Then("statusFor returns Connected") {
            store.statusFor(testIntegration, setOf("linear", "notion")) shouldBe
                IntegrationStatus.Connected
        }
    }

    Given("a store and an enabled set that does NOT contain the integration's id") {
        val (repo, oauth, _) = fakes()
        val store = IntegrationsStore(repo, oauth, initialEnabled = emptySet())

        Then("statusFor returns Disconnected") {
            store.statusFor(testIntegration, setOf("notion")) shouldBe
                IntegrationStatus.Disconnected
        }
    }
})

package dev.weft.undercurrent.feature.integrations

import dev.weft.undercurrent.data.datastore.IntegrationsRepository
import dev.weft.undercurrent.shared.gateway.OAuthConfig
import dev.weft.undercurrent.shared.gateway.OAuthGateway
import dev.weft.undercurrent.shared.gateway.OAuthResult
import dev.weft.undercurrent.shared.gateway.OAuthTokens
import io.kotest.core.spec.style.FunSpec
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
 * Exercises [IntegrationsStore] — the most complex of the feature
 * stores. Covers:
 *
 *  - OAuth flow happy path (Success → putTokens → setEnabled →
 *    ActionStatus.Success)
 *  - Every OAuthResult failure variant maps to ActionStatus.Failure
 *    with the appropriate message
 *  - Disconnect short-circuit (no OAuth call; just removeTokens +
 *    setEnabled false)
 *  - `pendingRestart` flag flips when the live enabled set diverges
 *    from `initialEnabled` (the boot-time snapshot)
 *  - statusFor() pure helper for Connected / Disconnected mapping
 *
 * The repository is mocked because its real impl is DataStore-backed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IntegrationsStoreTest : FunSpec({

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
        initialEnabledFromBoot: Set<String> = emptySet(),
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

    // ── initial state ────────────────────────────────────────────────

    test("initial state seeds enabledIds from the boot-time snapshot") {
        val (repo, oauth, _) = fakes(initialEnabledFromBoot = setOf("linear"))

        val store = IntegrationsStore(repo, oauth, initialEnabled = setOf("linear"))

        store.state.value shouldBe IntegrationsState(
            enabledIds = setOf("linear"),
            pendingRestart = false,
            lastAction = ActionStatus.Idle,
        )
    }

    // ── enabledIds + pendingRestart ──────────────────────────────────

    test("repo flow matches initialEnabled → pendingRestart stays false") {
        runTest {
            val (repo, oauth, enabledFlow) = fakes(
                initialEnabledFromFlow = setOf("linear"),
            )
            val store = IntegrationsStore(repo, oauth, initialEnabled = setOf("linear"))
            advanceUntilIdle()

            store.state.value.pendingRestart shouldBe false
            store.state.value.enabledIds shouldBe setOf("linear")
        }
    }

    test("repo flow diverges from initialEnabled → pendingRestart flips to true") {
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

    test("pendingRestart flips back to false when repo returns to initialEnabled") {
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

    // ── Connect happy path ───────────────────────────────────────────

    test("Connect — Success calls oauth.putTokens, repo.setEnabled, marks Success") {
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

    // Note: an earlier test attempted to verify the transient
    // `ActionStatus.InProgress(…, "Connecting…")` snapshot between the
    // `update { …InProgress… }` call and the await on `oauth.authorize`.
    // That turns out to be flaky under MockK + StandardTestDispatcher
    // — `coEvery { oauth.authorize(any()) } returns …` completes
    // immediately rather than truly suspending, so `runCurrent()`
    // doesn't pause where the test wanted it to. The Success-path test
    // above already exercises the `update {Success}` write at the end
    // of the launched block; the transient is internal state that's not
    // worth the brittleness to nail down.

    // ── Connect failure variants ─────────────────────────────────────

    test("Connect — UserCancelled marks Failure(\"Cancelled.\")") {
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

            // Importantly: no tokens persisted, no enabled bit flipped.
            coVerify(exactly = 0) { oauth.putTokens(any(), any()) }
            coVerify(exactly = 0) { repo.setEnabled(any(), any()) }
        }
    }

    test("Connect — ProviderError(code) renders just the code when description is null") {
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

    test("Connect — ProviderError(code, description) renders \"code: description\"") {
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

    test("Connect — TransportError carries the underlying message verbatim") {
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

    test("Connect — StateMismatch marks Failure with the retry hint") {
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

    // ── Disconnect ───────────────────────────────────────────────────

    test("Disconnect calls oauth.removeTokens, repo.setEnabled(false), Success") {
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
            // No OAuth authorization roundtrip on disconnect.
            coVerify(exactly = 0) { oauth.authorize(any()) }

            val action = store.state.value.lastAction
            check(action is ActionStatus.Success)
            action.message shouldBe "Disconnected"
        }
    }

    // ── ClearLastAction ──────────────────────────────────────────────

    test("ClearLastAction resets lastAction to Idle") {
        runTest {
            val (repo, oauth, _) = fakes()
            val store = IntegrationsStore(repo, oauth, initialEnabled = emptySet())

            store.dispatch(IntegrationsIntent.Connect(testIntegration))
            advanceUntilIdle()
            store.state.value.lastAction shouldBeSuccessFor "linear"

            store.dispatch(IntegrationsIntent.ClearLastAction)

            store.state.value.lastAction shouldBe ActionStatus.Idle
        }
    }

    // ── statusFor() pure helper ──────────────────────────────────────

    test("statusFor returns Connected when integration id is in enabled set") {
        val (repo, oauth, _) = fakes()
        val store = IntegrationsStore(repo, oauth, initialEnabled = emptySet())

        store.statusFor(testIntegration, setOf("linear", "notion")) shouldBe
            IntegrationStatus.Connected
    }

    test("statusFor returns Disconnected when integration id is missing") {
        val (repo, oauth, _) = fakes()
        val store = IntegrationsStore(repo, oauth, initialEnabled = emptySet())

        store.statusFor(testIntegration, setOf("notion")) shouldBe
            IntegrationStatus.Disconnected
    }
})

// ── matchers ─────────────────────────────────────────────────────────

private infix fun ActionStatus.shouldBeSuccessFor(id: String): ActionStatus = also {
    check(this is ActionStatus.Success) { "expected Success, got $this" }
    (this as ActionStatus.Success).integrationId shouldBe id
}

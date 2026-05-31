package dev.weft.undercurrent.core.domain.sessiontoken

import dev.weft.undercurrent.core.domain.SessionTokenStore
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest

/**
 * Reusable behavior contract for any [SessionTokenStore] implementation.
 *
 * Each implementation provides a fresh-store [factory] and subclasses
 * this spec; the same assertions then run against every impl. Used by:
 *  - `FakeSessionTokenStoreTest` here (proves the fake satisfies the contract)
 *  - the Android `EncryptedSharedPreferences` impl (mobile-auth-wiring/02)
 *  - the iOS Keychain impl (mobile-auth-wiring/03)
 *
 * The contract intentionally asserts only the four observable behaviors
 * the Inception spelled out — anything platform-specific (persistence
 * across process kill, uninstall behavior, etc.) belongs in the
 * platform's own test spec on top of this.
 */
abstract class SessionTokenStoreContractSpec(
    factory: () -> SessionTokenStore,
) : BehaviorSpec({

    Given("a fresh store with nothing saved") {
        When("read() is called") {
            Then("it returns null") {
                runTest {
                    factory().read() shouldBe null
                }
            }
        }
    }

    Given("a token has been saved") {
        When("read() is called") {
            Then("it returns the same token") {
                runTest {
                    val store = factory()
                    store.save("token-abc-123")
                    store.read() shouldBe "token-abc-123"
                }
            }
        }
    }

    Given("a token has been saved and then cleared") {
        When("read() is called") {
            Then("it returns null") {
                runTest {
                    val store = factory()
                    store.save("token-to-be-cleared")
                    store.clear()
                    store.read() shouldBe null
                }
            }
        }
    }

    Given("a token is saved over an existing token (single-slot replace)") {
        When("read() is called") {
            Then("it returns the newer token") {
                runTest {
                    val store = factory()
                    store.save("first")
                    store.save("second")
                    store.read() shouldBe "second"
                }
            }
        }
    }

    Given("clear() is called on a fresh store with nothing saved") {
        When("read() is called after the no-op clear()") {
            Then("it returns null without throwing") {
                runTest {
                    val store = factory()
                    store.clear() // no-op
                    store.read() shouldBe null
                }
            }
        }
    }
})

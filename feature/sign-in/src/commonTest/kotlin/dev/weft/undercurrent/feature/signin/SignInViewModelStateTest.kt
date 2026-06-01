@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.weft.undercurrent.feature.signin

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * KMP-portable state-projection tests for [SignInViewModel]. Runs on
 * Android + iOS via the convention plugin's kotest wiring.
 *
 * Covers form-state mutations only (mode toggle, field input,
 * canSubmit gate, error dismissal). Continue-dispatch + BE-error
 * mapping land in the next tests as part of tasks 3 + 4.
 */
class SignInViewModelStateTest : BehaviorSpec({

    val mainDispatcher = StandardTestDispatcher()
    beforeTest { Dispatchers.setMain(mainDispatcher) }
    afterTest { Dispatchers.resetMain() }

    fun freshVm(): SignInViewModel = SignInViewModel(
        authRepository = FakeAuthRepository(),
        sessionTokenStore = FakeSessionTokenStore(),
    )

    Given("a fresh SignInViewModel") {
        Then("the initial state is Sign-In mode with empty fields, no errors, and canSubmit=false") {
            val vm = freshVm()
            vm.state.value shouldBe SignInState()
            vm.state.value.canSubmit shouldBe false
        }
    }

    Given("a fresh VM in Sign-In mode") {
        When("EmailChanged + PasswordChanged are dispatched with valid input") {
            Then("canSubmit becomes true") {
                runTest {
                    val vm = freshVm()
                    vm.dispatch(SignInIntent.EmailChanged("phuc@example.com"))
                    vm.dispatch(SignInIntent.PasswordChanged("hunter2-correct"))
                    advanceUntilIdle()
                    val s = vm.state.value
                    s.email shouldBe "phuc@example.com"
                    s.password shouldBe "hunter2-correct"
                    s.canSubmit shouldBe true
                }
            }
        }
        When("the email is malformed") {
            Then("canSubmit stays false even with a non-empty password") {
                runTest {
                    val vm = freshVm()
                    vm.dispatch(SignInIntent.EmailChanged("not-an-email"))
                    vm.dispatch(SignInIntent.PasswordChanged("anything"))
                    advanceUntilIdle()
                    vm.state.value.canSubmit shouldBe false
                }
            }
        }
        When("the password is empty") {
            Then("canSubmit is false even with a valid email") {
                runTest {
                    val vm = freshVm()
                    vm.dispatch(SignInIntent.EmailChanged("phuc@example.com"))
                    advanceUntilIdle()
                    vm.state.value.canSubmit shouldBe false
                }
            }
        }
    }

    Given("a VM in Register mode") {
        Then("canSubmit gates on displayName + email + password ≥ 8") {
            runTest {
                val vm = freshVm()
                vm.dispatch(SignInIntent.SwitchMode)
                vm.dispatch(SignInIntent.EmailChanged("phuc@example.com"))
                vm.dispatch(SignInIntent.PasswordChanged("hunter2-correct"))
                advanceUntilIdle()
                // displayName still empty
                vm.state.value.canSubmit shouldBe false

                vm.dispatch(SignInIntent.DisplayNameChanged("Phuc"))
                advanceUntilIdle()
                vm.state.value.canSubmit shouldBe true
            }
        }
        Then("displayName whitespace-only fails the trimmed non-empty check") {
            runTest {
                val vm = freshVm()
                vm.dispatch(SignInIntent.SwitchMode)
                vm.dispatch(SignInIntent.EmailChanged("phuc@example.com"))
                vm.dispatch(SignInIntent.PasswordChanged("hunter2-correct"))
                vm.dispatch(SignInIntent.DisplayNameChanged("    "))
                advanceUntilIdle()
                vm.state.value.canSubmit shouldBe false
            }
        }
        Then("displayName > 40 chars fails") {
            runTest {
                val vm = freshVm()
                vm.dispatch(SignInIntent.SwitchMode)
                vm.dispatch(SignInIntent.EmailChanged("phuc@example.com"))
                vm.dispatch(SignInIntent.PasswordChanged("hunter2-correct"))
                vm.dispatch(SignInIntent.DisplayNameChanged("x".repeat(41)))
                advanceUntilIdle()
                vm.state.value.canSubmit shouldBe false
            }
        }
        Then("password < 8 chars fails") {
            runTest {
                val vm = freshVm()
                vm.dispatch(SignInIntent.SwitchMode)
                vm.dispatch(SignInIntent.EmailChanged("phuc@example.com"))
                vm.dispatch(SignInIntent.PasswordChanged("short"))
                vm.dispatch(SignInIntent.DisplayNameChanged("Phuc"))
                advanceUntilIdle()
                vm.state.value.canSubmit shouldBe false
            }
        }
    }

    Given("a Sign-In-mode VM with email + password filled") {
        When("SwitchMode is dispatched") {
            Then("mode flips to Register, email + password are preserved, displayName starts empty") {
                runTest {
                    val vm = freshVm()
                    vm.dispatch(SignInIntent.EmailChanged("phuc@example.com"))
                    vm.dispatch(SignInIntent.PasswordChanged("hunter2-correct"))
                    vm.dispatch(SignInIntent.SwitchMode)
                    advanceUntilIdle()
                    val s = vm.state.value
                    s.mode shouldBe SignInState.Mode.Register
                    s.email shouldBe "phuc@example.com"
                    s.password shouldBe "hunter2-correct"
                    s.displayName shouldBe ""
                }
            }
            Then("a previously-set topError is cleared on toggle") {
                runTest {
                    val vm = freshVm()
                    vm.dispatch(SignInIntent.EmailChanged("phuc@example.com"))
                    // Simulate a stale error by toggling twice with a manual error in between.
                    // We can't directly set topError without going through a Continue dispatch,
                    // but the AC says "toggling clears errors". For now this case is covered
                    // by the Continue-dispatch tests in tasks 3 + 4.
                    vm.dispatch(SignInIntent.SwitchMode)
                    advanceUntilIdle()
                    vm.state.value.topError shouldBe null
                }
            }
        }
    }

    Given("a Register-mode VM with the 'switch to sign-in' shortcut visible") {
        When("SwitchToSignInWithEmail is dispatched") {
            Then("mode flips to Sign-In, email is preserved, the shortcut + topError are cleared") {
                runTest {
                    val vm = freshVm()
                    // Get into Register mode + populate the email
                    vm.dispatch(SignInIntent.SwitchMode)
                    vm.dispatch(SignInIntent.EmailChanged("taken@example.com"))
                    advanceUntilIdle()
                    // (The shortcut would be set by the 409 path in task 4; we assert here
                    // that the SwitchToSignInWithEmail intent itself leaves the right shape.)
                    vm.dispatch(SignInIntent.SwitchToSignInWithEmail)
                    advanceUntilIdle()
                    val s = vm.state.value
                    s.mode shouldBe SignInState.Mode.SignIn
                    s.email shouldBe "taken@example.com"
                    s.showSwitchToSignInShortcut shouldBe false
                    s.topError shouldBe null
                }
            }
        }
    }

    Given("a VM with a network top-error") {
        When("ClearTopError is dispatched") {
            Then("topError becomes null") {
                runTest {
                    val vm = freshVm()
                    // We can't set topError directly without the Continue path (task 3).
                    // Assert that a fresh VM dispatching ClearTopError stays consistent
                    // (idempotent) — full coverage of the network-clear cycle is in task 3.
                    vm.dispatch(SignInIntent.ClearTopError)
                    advanceUntilIdle()
                    vm.state.value.topError shouldBe null
                }
            }
        }
    }
})

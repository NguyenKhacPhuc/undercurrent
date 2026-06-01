@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.weft.undercurrent.feature.signin

import app.cash.turbine.test
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
import dev.weft.undercurrent.core.domain.auth.dto.AuthResponse
import dev.weft.undercurrent.core.domain.auth.dto.SessionDto
import dev.weft.undercurrent.core.ext.Result
import dev.weft.undercurrent.data.network.common.ApiException
import dev.weft.undercurrent.data.network.common.NetworkException
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class SignInViewModelStateTest : BehaviorSpec({

    val mainDispatcher = StandardTestDispatcher()
    beforeTest { Dispatchers.setMain(mainDispatcher) }
    afterTest { Dispatchers.resetMain() }

    fun freshVm(): SignInViewModel {
        val repo = mock<AuthRepository>()
        val store = mock<SessionTokenStore> {
            everySuspend { save(any()) } returns Unit
        }
        return SignInViewModel(authRepository = repo, sessionTokenStore = store)
    }

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
                    s.topError shouldBe null
                }
            }
        }
    }

    Given("a Register-mode VM with the email populated") {
        When("SwitchToSignInWithEmail is dispatched") {
            Then("mode flips to Sign-In, email is preserved, shortcut + topError cleared") {
                runTest {
                    val vm = freshVm()
                    vm.dispatch(SignInIntent.SwitchMode)
                    vm.dispatch(SignInIntent.EmailChanged("taken@example.com"))
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

    Given("a fresh VM") {
        When("ClearTopError is dispatched") {
            Then("topError stays null (idempotent)") {
                runTest {
                    val vm = freshVm()
                    vm.dispatch(SignInIntent.ClearTopError)
                    advanceUntilIdle()
                    vm.state.value.topError shouldBe null
                }
            }
        }
    }

    fun signInVm(
        signInStub: (AuthRepository) -> Unit = {},
    ): Triple<SignInViewModel, AuthRepository, SessionTokenStore> {
        val repo = mock<AuthRepository>().also(signInStub)
        val store = mock<SessionTokenStore> { everySuspend { save(any()) } returns Unit }
        return Triple(
            SignInViewModel(authRepository = repo, sessionTokenStore = store),
            repo,
            store,
        )
    }

    fun fillSignIn(vm: SignInViewModel) {
        vm.dispatch(SignInIntent.EmailChanged("phuc@example.com"))
        vm.dispatch(SignInIntent.PasswordChanged("hunter2-correct"))
    }

    Given("Sign-In Continue with canSubmit=false") {
        Then("AuthRepository.signIn is NEVER called and state is unchanged") {
            runTest {
                val (vm, repo, store) = signInVm()
                vm.dispatch(SignInIntent.Continue)
                advanceUntilIdle()
                verify(exactly(0)) { repo.signIn(any(), any()) }
                verifySuspend(exactly(0)) { store.save(any()) }
                vm.loading.value shouldBe false
            }
        }
    }

    Given("Sign-In Continue with valid form and a 200 BE response") {
        Then("the bearer is persisted via save() and Effect.SignedIn is emitted") {
            runTest {
                val authResponse = AuthResponse(
                    account = AccountDto("acct.abc", "Phuc", "phuc@example.com", 1L),
                    session = SessionDto(token = "tk-success", expiresAtMs = 2L),
                )
                val (vm, repo, store) = signInVm { r ->
                    every { r.signIn(any(), any()) } returns flowOf(Result.Loading, Result.Success(authResponse))
                }

                vm.effects.test {
                    fillSignIn(vm)
                    vm.dispatch(SignInIntent.Continue)
                    advanceUntilIdle()
                    awaitItem() shouldBe SignInEffect.SignedIn
                    cancelAndIgnoreRemainingEvents()
                }

                verify(exactly(1)) { repo.signIn("phuc@example.com", "hunter2-correct") }
                verifySuspend(exactly(1)) { store.save("tk-success") }
                val s = vm.state.value
                vm.loading.value shouldBe false
                s.topError shouldBe null
            }
        }
    }

    Given("Sign-In Continue with a flow that hangs after Result.Loading") {
        Then("vm.loading observes the false→true→false transition across the call") {
            runTest {
                val resultChannel = Channel<Result<AuthResponse>>(Channel.UNLIMITED)
                resultChannel.send(Result.Loading)
                val (vm, _, _) = signInVm { r ->
                    every { r.signIn(any(), any()) } returns resultChannel.receiveAsFlow()
                }

                vm.loading.test {
                    awaitItem() shouldBe false
                    fillSignIn(vm)
                    vm.dispatch(SignInIntent.Continue)
                    advanceUntilIdle()
                    awaitItem() shouldBe true
                    resultChannel.send(
                        Result.Success(
                            AuthResponse(
                                account = AccountDto("acct.a", "A", "a@b.c", 1L),
                                session = SessionDto("tk", 2L),
                            ),
                        ),
                    )
                    resultChannel.close()
                    advanceUntilIdle()
                    awaitItem() shouldBe false
                }
            }
        }
    }

    Given("Sign-In Continue with a 401 BE response") {
        Then("topError becomes InvalidCredentials and loading flips off") {
            runTest {
                val err = ApiException(
                    code = "unauthenticated",
                    apiMessage = "Invalid email or password",
                    endpoint = "/v1/auth/sign-in",
                    httpStatus = 401,
                )
                val (vm, _, store) = signInVm { r ->
                    every { r.signIn(any(), any()) } returns flowOf(Result.Loading, Result.Error(err))
                }
                fillSignIn(vm)
                vm.dispatch(SignInIntent.Continue)
                advanceUntilIdle()
                vm.loading.value shouldBe false
                vm.state.value.topError shouldBe TopError.InvalidCredentials
                verifySuspend(exactly(0)) { store.save(any()) }
            }
        }
    }

    Given("Sign-In Continue with a 429 BE response") {
        Then("topError becomes RateLimited") {
            runTest {
                val err = ApiException(
                    code = "rate_limited",
                    apiMessage = "Too many failed sign-in attempts.",
                    endpoint = "/v1/auth/sign-in",
                    httpStatus = 429,
                )
                val (vm, _, _) = signInVm { r ->
                    every { r.signIn(any(), any()) } returns flowOf(Result.Loading, Result.Error(err))
                }
                fillSignIn(vm)
                vm.dispatch(SignInIntent.Continue)
                advanceUntilIdle()
                vm.state.value.topError shouldBe TopError.RateLimited
            }
        }
    }

    Given("Sign-In Continue with a 400 BE response (generic)") {
        Then("topError carries the BE message above the form") {
            runTest {
                val err = ApiException(
                    code = "invalid_request",
                    apiMessage = "email and password are required",
                    endpoint = "/v1/auth/sign-in",
                    httpStatus = 400,
                )
                val (vm, _, _) = signInVm { r ->
                    every { r.signIn(any(), any()) } returns flowOf(Result.Loading, Result.Error(err))
                }
                fillSignIn(vm)
                vm.dispatch(SignInIntent.Continue)
                advanceUntilIdle()
                val top = vm.state.value.topError.shouldBeInstanceOf<TopError.Message>()
                top.message shouldBe "email and password are required"
            }
        }
    }

    Given("Sign-In Continue with a NetworkException") {
        Then("topError becomes Network so the UI can show Retry") {
            runTest {
                val (vm, _, _) = signInVm { r ->
                    every { r.signIn(any(), any()) } returns flowOf(Result.Loading, Result.Error(NetworkException()))
                }
                fillSignIn(vm)
                vm.dispatch(SignInIntent.Continue)
                advanceUntilIdle()
                vm.state.value.topError shouldBe TopError.Network
            }
        }
    }

    fun registerVm(
        signUpStub: (AuthRepository) -> Unit = {},
    ): Triple<SignInViewModel, AuthRepository, SessionTokenStore> {
        val repo = mock<AuthRepository>().also(signUpStub)
        val store = mock<SessionTokenStore> { everySuspend { save(any()) } returns Unit }
        return Triple(
            SignInViewModel(authRepository = repo, sessionTokenStore = store),
            repo,
            store,
        )
    }

    fun fillRegister(vm: SignInViewModel) {
        vm.dispatch(SignInIntent.SwitchMode)
        vm.dispatch(SignInIntent.DisplayNameChanged("Phuc"))
        vm.dispatch(SignInIntent.EmailChanged("phuc@example.com"))
        vm.dispatch(SignInIntent.PasswordChanged("hunter2-correct"))
    }

    Given("Register Continue with !canSubmit (displayName empty)") {
        Then("AuthRepository.signUp is NEVER called") {
            runTest {
                val (vm, repo, _) = registerVm()
                vm.dispatch(SignInIntent.SwitchMode)
                vm.dispatch(SignInIntent.EmailChanged("phuc@example.com"))
                vm.dispatch(SignInIntent.PasswordChanged("hunter2-correct"))
                vm.dispatch(SignInIntent.Continue)
                advanceUntilIdle()
                verify(exactly(0)) { repo.signUp(any(), any(), any()) }
            }
        }
    }

    Given("Register Continue with valid form and a 201 BE response") {
        Then("the bearer is persisted, Effect.SignedIn is emitted, signUp got the right args") {
            runTest {
                val ok = AuthResponse(
                    account = AccountDto("acct.new", "Phuc", "phuc@example.com", 1L),
                    session = SessionDto(token = "tk-new", expiresAtMs = 2L),
                )
                val (vm, repo, store) = registerVm { r ->
                    every { r.signUp(any(), any(), any()) } returns flowOf(Result.Loading, Result.Success(ok))
                }

                vm.effects.test {
                    fillRegister(vm)
                    vm.dispatch(SignInIntent.Continue)
                    advanceUntilIdle()
                    awaitItem() shouldBe SignInEffect.SignedIn
                    cancelAndIgnoreRemainingEvents()
                }

                verify(exactly(1)) { repo.signUp("Phuc", "phuc@example.com", "hunter2-correct") }
                verifySuspend(exactly(1)) { store.save("tk-new") }
                vm.loading.value shouldBe false
            }
        }
    }

    Given("Register Continue with a 400 BE response carrying field details") {
        Then("fieldErrors are populated and no topError is shown above the form") {
            runTest {
                val err = ApiException(
                    code = "invalid_request",
                    apiMessage = "One or more fields are invalid",
                    details = mapOf("email" to "must be a valid email address", "password" to "must be at least 8 characters"),
                    endpoint = "/v1/auth/sign-up",
                    httpStatus = 400,
                )
                val (vm, _, _) = registerVm { r ->
                    every { r.signUp(any(), any(), any()) } returns flowOf(Result.Loading, Result.Error(err))
                }
                fillRegister(vm)
                vm.dispatch(SignInIntent.Continue)
                advanceUntilIdle()
                val s = vm.state.value
                s.fieldErrors shouldBe mapOf(
                    "email" to "must be a valid email address",
                    "password" to "must be at least 8 characters",
                )
                s.topError shouldBe null
            }
        }
    }

    Given("Register Continue with a 400 BE response WITHOUT field details") {
        Then("topError carries the BE message above the form") {
            runTest {
                val err = ApiException(
                    code = "invalid_request",
                    apiMessage = "Request body is missing or malformed",
                    details = null,
                    endpoint = "/v1/auth/sign-up",
                    httpStatus = 400,
                )
                val (vm, _, _) = registerVm { r ->
                    every { r.signUp(any(), any(), any()) } returns flowOf(Result.Loading, Result.Error(err))
                }
                fillRegister(vm)
                vm.dispatch(SignInIntent.Continue)
                advanceUntilIdle()
                val top = vm.state.value.topError.shouldBeInstanceOf<TopError.Message>()
                top.message shouldBe "Request body is missing or malformed"
                vm.state.value.fieldErrors shouldBe emptyMap()
            }
        }
    }

    Given("Register Continue with a 409 email_already_registered BE response") {
        Then("topError carries the BE message AND the Switch-to-Sign-In shortcut is surfaced") {
            runTest {
                val err = ApiException(
                    code = "email_already_registered",
                    apiMessage = "An account with this email already exists",
                    endpoint = "/v1/auth/sign-up",
                    httpStatus = 409,
                )
                val (vm, _, _) = registerVm { r ->
                    every { r.signUp(any(), any(), any()) } returns flowOf(Result.Loading, Result.Error(err))
                }
                fillRegister(vm)
                vm.dispatch(SignInIntent.Continue)
                advanceUntilIdle()
                val s = vm.state.value
                val top = s.topError.shouldBeInstanceOf<TopError.Message>()
                top.message shouldBe "An account with this email already exists"
                s.showSwitchToSignInShortcut shouldBe true
            }
        }
    }

    Given("Register Continue with a NetworkException") {
        Then("topError becomes Network") {
            runTest {
                val (vm, _, _) = registerVm { r ->
                    every { r.signUp(any(), any(), any()) } returns flowOf(Result.Loading, Result.Error(NetworkException()))
                }
                fillRegister(vm)
                vm.dispatch(SignInIntent.Continue)
                advanceUntilIdle()
                vm.state.value.topError shouldBe TopError.Network
            }
        }
    }

    Given("after a 409 surfaces the Switch shortcut, SwitchToSignInWithEmail is dispatched") {
        Then("the VM flips to Sign-In mode with the email preserved and the shortcut cleared") {
            runTest {
                val err = ApiException(
                    code = "email_already_registered",
                    apiMessage = "An account with this email already exists",
                    endpoint = "/v1/auth/sign-up",
                    httpStatus = 409,
                )
                val (vm, _, _) = registerVm { r ->
                    every { r.signUp(any(), any(), any()) } returns flowOf(Result.Loading, Result.Error(err))
                }
                fillRegister(vm)
                vm.dispatch(SignInIntent.Continue)
                advanceUntilIdle()
                vm.state.value.showSwitchToSignInShortcut shouldBe true

                vm.dispatch(SignInIntent.SwitchToSignInWithEmail)
                advanceUntilIdle()
                val s = vm.state.value
                s.mode shouldBe SignInState.Mode.SignIn
                s.email shouldBe "phuc@example.com"
                s.showSwitchToSignInShortcut shouldBe false
                s.topError shouldBe null
            }
        }
    }
})

package dev.weft.undercurrent.core.navigation

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * BDD coverage for [NavigationViewModel]. Pure synchronous dispatch —
 * the VM doesn't extend [dev.weft.undercurrent.shared.mvi.MviViewModel],
 * so there's no coroutine machinery to drain. Every assertion is about
 * the back-stack shape after a sequence of [NavigationIntent] dispatches.
 *
 * KMP — runs on Android + iOS.
 */
class NavigationViewModelTest : BehaviorSpec({

    Given("a freshly constructed NavigationViewModel") {
        val vm = NavigationViewModel()

        Then("the back stack starts seeded with Loading") {
            vm.backStack.toList() shouldContainExactly listOf(Screen.Loading)
        }

        Then("the current screen is Loading") {
            vm.current shouldBe Screen.Loading
        }
    }

    Given("a fresh VM with a single Navigate(Chat) dispatched") {
        val vm = NavigationViewModel()
        vm.dispatch(NavigationIntent.Navigate(Screen.Chat))

        Then("Chat is pushed on top of Loading") {
            vm.backStack.toList() shouldContainExactly listOf(Screen.Loading, Screen.Chat)
        }

        Then("current points at Chat") {
            vm.current shouldBe Screen.Chat
        }
    }

    Given("a VM where Navigate is dispatched twice for the same screen") {
        val vm = NavigationViewModel()
        vm.dispatch(NavigationIntent.Navigate(Screen.Chat))
        vm.dispatch(NavigationIntent.Navigate(Screen.Chat))

        Then("the duplicate push is suppressed — guards re-entry") {
            vm.backStack.toList() shouldContainExactly listOf(Screen.Loading, Screen.Chat)
        }
    }

    Given("a VM with Loading → Chat → Settings on the stack") {
        val vm = NavigationViewModel()
        vm.dispatch(NavigationIntent.Navigate(Screen.Chat))
        vm.dispatch(NavigationIntent.Navigate(Screen.Settings))

        When("Back is dispatched once") {
            vm.dispatch(NavigationIntent.Back)

            Then("Settings pops off and current returns to Chat") {
                vm.backStack.toList() shouldContainExactly listOf(Screen.Loading, Screen.Chat)
                vm.current shouldBe Screen.Chat
            }
        }
    }

    Given("a VM at root (single-entry stack)") {
        val vm = NavigationViewModel()

        When("Back is dispatched") {
            vm.dispatch(NavigationIntent.Back)

            Then("the root entry is preserved — Back is a no-op when nothing can pop") {
                vm.backStack.toList() shouldContainExactly listOf(Screen.Loading)
            }
        }
    }

    Given("a VM with multiple entries pushed") {
        val vm = NavigationViewModel()
        vm.dispatch(NavigationIntent.Navigate(Screen.Onboarding))
        vm.dispatch(NavigationIntent.Navigate(Screen.KeyPaste))
        vm.dispatch(NavigationIntent.Navigate(Screen.Chat))

        When("NavigateAndClear(Chat) is dispatched") {
            vm.dispatch(NavigationIntent.NavigateAndClear(Screen.Chat))

            Then("the stack is reset to exactly one entry") {
                vm.backStack.toList() shouldContainExactly listOf(Screen.Chat)
                vm.current shouldBe Screen.Chat
            }
        }
    }

    Given("a fresh VM") {
        val vm = NavigationViewModel()

        When("NavigateAndClear is dispatched on a single-entry stack") {
            vm.dispatch(NavigationIntent.NavigateAndClear(Screen.KeyPaste))

            Then("the seed entry is replaced by the requested screen") {
                vm.backStack.toList() shouldContainExactly listOf(Screen.KeyPaste)
            }
        }
    }

    Given("a VM exercised across every intent variant") {
        val vm = NavigationViewModel()
        vm.dispatch(NavigationIntent.Navigate(Screen.Chat))
        vm.dispatch(NavigationIntent.Navigate(Screen.Settings))
        vm.dispatch(NavigationIntent.Back)
        vm.dispatch(NavigationIntent.NavigateAndClear(Screen.Memories))

        Then("the final stack reflects only the last NavigateAndClear") {
            vm.backStack.toList() shouldContainExactly listOf(Screen.Memories)
        }
    }
})

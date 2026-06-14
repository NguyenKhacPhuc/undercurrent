package dev.weft.undercurrent.feature.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.weft.undercurrent.core.domain.ModelCatalogRepository
import dev.weft.undercurrent.core.domain.OnboardingRepository
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.feature.settings.providers.ProviderViewModel
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * KMP-portable BDD coverage for [OnboardingViewModel]. The single
 * intent does two observable things: persists the onboarding-complete
 * flag through the use case and resets the nav stack to KeyPaste.
 *
 * Uses real [CompleteOnboardingUseCase] + [OnboardingRepository] over
 * an in-memory [FakePreferencesDataStore] (no MockK — commonTest).
 * The DataStore's [DataStore.data] flow is the source of truth for
 * "did the use case fire" — we read it after dispatch.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest : BehaviorSpec({

    val mainDispatcher = StandardTestDispatcher()
    beforeTest { Dispatchers.setMain(mainDispatcher) }
    afterTest { Dispatchers.resetMain() }

    Given("a fresh OnboardingViewModel wired to an empty DataStore") {
        Then("the DataStore reports onboarding incomplete before any dispatch") {
            runTest {
                val ds = FakePreferencesDataStore()
                val repo = OnboardingRepository(ds)

                repo.completedFlow.first() shouldBe false
            }
        }
    }

    Given("a VM where CompleteOnboarding is dispatched") {
        Then("the underlying DataStore flips completed=true (use case fired)") {
            runTest {
                val ds = FakePreferencesDataStore()
                val repo = OnboardingRepository(ds)
                val navVm = NavigationViewModel()
                val vm = OnboardingViewModel(
                    repo = repo,
                    navigationVm = navVm,
                    provider = mock<ProviderViewModel>(),
                    catalog = mock<ModelCatalogRepository> {
                        every { modelsForProvider(any()) } returns emptyList()
                    },
                )

                vm.dispatch(OnboardingIntent.CompleteOnboarding)
                advanceUntilIdle()

                repo.completedFlow.first() shouldBe true
            }
        }

        Then("the nav stack is reset to KeyPaste alone (NavigateAndClear fired)") {
            runTest {
                val ds = FakePreferencesDataStore()
                val repo = OnboardingRepository(ds)
                val navVm = NavigationViewModel()
                navVm.dispatch(dev.weft.undercurrent.core.navigation.NavigationIntent.Navigate(Screen.Onboarding))
                val vm = OnboardingViewModel(
                    repo = repo,
                    navigationVm = navVm,
                    provider = mock<ProviderViewModel>(),
                    catalog = mock<ModelCatalogRepository> {
                        every { modelsForProvider(any()) } returns emptyList()
                    },
                )

                vm.dispatch(OnboardingIntent.CompleteOnboarding)
                advanceUntilIdle()

                navVm.backStack.toList() shouldContainExactly listOf(Screen.KeyPaste)
                navVm.current shouldBe Screen.KeyPaste
            }
        }
    }

    Given("a VM where CompleteOnboarding is dispatched twice") {
        Then("the use case remains idempotent and the nav stack stays at KeyPaste") {
            runTest {
                val ds = FakePreferencesDataStore()
                val repo = OnboardingRepository(ds)
                val navVm = NavigationViewModel()
                val vm = OnboardingViewModel(
                    repo = repo,
                    navigationVm = navVm,
                    provider = mock<ProviderViewModel>(),
                    catalog = mock<ModelCatalogRepository> {
                        every { modelsForProvider(any()) } returns emptyList()
                    },
                )

                vm.dispatch(OnboardingIntent.CompleteOnboarding)
                vm.dispatch(OnboardingIntent.CompleteOnboarding)
                advanceUntilIdle()

                repo.completedFlow.first() shouldBe true
                navVm.backStack.toList() shouldContainExactly listOf(Screen.KeyPaste)
            }
        }
    }
})

/**
 * In-memory [DataStore] of [Preferences] — duplicate of the fake
 * in `:core:domain`'s commonTest. Kept local so this module doesn't
 * need a test-only project dep.
 */
private class FakePreferencesDataStore(
    initial: Preferences = emptyPreferences(),
) : DataStore<Preferences> {

    private val state = MutableStateFlow(initial)
    private val writeLock = Mutex()

    override val data: Flow<Preferences> get() = state

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences,
    ): Preferences = writeLock.withLock {
        val updated = transform(state.value)
        state.value = updated
        updated
    }
}

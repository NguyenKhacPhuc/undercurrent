package dev.weft.undercurrent.feature.theme

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import dev.weft.undercurrent.core.domain.ThemeRepository
import dev.weft.undercurrent.core.model.AppPalette
import dev.weft.undercurrent.core.model.ThemeMode
import dev.weft.undercurrent.core.model.ThemePrefs
import io.kotest.core.spec.style.BehaviorSpec
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
 * KMP-portable BDD coverage for [ThemeViewModel]. Exercises:
 *   - init-time collection from [ObserveThemePrefsUseCase] into state
 *   - [ThemeIntent.SetPalette] forwarding to the repository
 *   - [ThemeIntent.SetThemeMode] forwarding to the repository
 *
 * Uses real use cases over a real [ThemeRepository] backed by an
 * in-memory [FakePreferencesDataStore] — no MockK (commonTest must
 * compile + run on iOS).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest : BehaviorSpec({

    val mainDispatcher = StandardTestDispatcher()
    beforeTest { Dispatchers.setMain(mainDispatcher) }
    afterTest { Dispatchers.resetMain() }

    Given("a ThemeViewModel wired to an empty DataStore") {
        Then("initial state defaults to ThemePrefs.Default after the init flow drains") {
            runTest {
                val repo = ThemeRepository(FakePreferencesDataStore())
                val vm = ThemeViewModel(repo = repo)
                advanceUntilIdle()

                vm.state.value.prefs shouldBe ThemePrefs.Default
            }
        }
    }

    Given("a ThemeViewModel with SetPalette(Newsprint) dispatched") {
        Then("state.prefs.palette propagates to Newsprint after the write round-trips") {
            runTest {
                val repo = ThemeRepository(FakePreferencesDataStore())
                val vm = ThemeViewModel(repo = repo)
                advanceUntilIdle()

                vm.dispatch(ThemeIntent.SetPalette(AppPalette.Newsprint))
                advanceUntilIdle()

                vm.state.value.prefs.palette shouldBe AppPalette.Newsprint
            }
        }

        Then("the underlying repository's prefsFlow reflects the new palette") {
            runTest {
                val repo = ThemeRepository(FakePreferencesDataStore())
                val vm = ThemeViewModel(repo = repo)
                advanceUntilIdle()

                vm.dispatch(ThemeIntent.SetPalette(AppPalette.SageOchre))
                advanceUntilIdle()

                repo.prefsFlow.first().palette shouldBe AppPalette.SageOchre
            }
        }
    }

    Given("a ThemeViewModel with SetThemeMode(Dark) dispatched") {
        Then("state.prefs.mode flips to Dark") {
            runTest {
                val repo = ThemeRepository(FakePreferencesDataStore())
                val vm = ThemeViewModel(repo = repo)
                advanceUntilIdle()

                vm.dispatch(ThemeIntent.SetThemeMode(ThemeMode.Dark))
                advanceUntilIdle()

                vm.state.value.prefs.mode shouldBe ThemeMode.Dark
            }
        }
    }

    Given("a ThemeViewModel with palette + mode both dispatched") {
        Then("both fields land independently — neither dispatch clobbers the other") {
            runTest {
                val repo = ThemeRepository(FakePreferencesDataStore())
                val vm = ThemeViewModel(repo = repo)
                advanceUntilIdle()

                vm.dispatch(ThemeIntent.SetPalette(AppPalette.WarmDarkAmber))
                vm.dispatch(ThemeIntent.SetThemeMode(ThemeMode.Light))
                advanceUntilIdle()

                vm.state.value.prefs.palette shouldBe AppPalette.WarmDarkAmber
                vm.state.value.prefs.mode shouldBe ThemeMode.Light
            }
        }
    }

    Given("a ThemeViewModel with SetPalette dispatched twice") {
        Then("the later write wins — state reflects the last palette") {
            runTest {
                val repo = ThemeRepository(FakePreferencesDataStore())
                val vm = ThemeViewModel(repo = repo)
                advanceUntilIdle()

                vm.dispatch(ThemeIntent.SetPalette(AppPalette.WarmDarkAmber))
                vm.dispatch(ThemeIntent.SetPalette(AppPalette.Vellum))
                advanceUntilIdle()

                vm.state.value.prefs.palette shouldBe AppPalette.Vellum
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

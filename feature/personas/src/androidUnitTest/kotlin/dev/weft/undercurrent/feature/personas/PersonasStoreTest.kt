package dev.weft.undercurrent.feature.personas

import dev.weft.undercurrent.core.model.BuiltInPersonas
import dev.weft.undercurrent.core.model.Persona
import dev.weft.undercurrent.core.model.PersonaKind
import dev.weft.undercurrent.data.datastore.PersonaRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
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
 * Exercises [PersonasStore]. The repo is mocked via MockK; the three
 * flow properties (`activeVoice`, `activeRole`, `customPersonas`) are
 * backed by `MutableStateFlow`s so the test can both seed initial values
 * and emit changes mid-test.
 *
 * Every `viewModelScope.launch` in [PersonasStore] dispatches on
 * `Dispatchers.Main`, which the test replaces with a
 * `StandardTestDispatcher`. `advanceUntilIdle()` drains pending
 * coroutines so suspend calls into the repo (e.g. `setActiveVoice`)
 * actually fire by the time the assertion runs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PersonasStoreTest : FunSpec({

    val mainDispatcher = StandardTestDispatcher()

    beforeTest { Dispatchers.setMain(mainDispatcher) }
    afterTest { Dispatchers.resetMain() }

    // ── helpers ──────────────────────────────────────────────────────

    fun fakeRepo(
        activeVoice: Persona = BuiltInPersonas.Default,
        activeRole: Persona? = null,
        customPersonas: List<Persona> = emptyList(),
    ): PersonaRepository {
        val repo = mockk<PersonaRepository>()
        every { repo.activeVoice } returns MutableStateFlow(activeVoice)
        every { repo.activeRole } returns MutableStateFlow(activeRole)
        every { repo.customPersonas } returns MutableStateFlow(customPersonas)
        coEvery { repo.setActiveVoice(any()) } returns Unit
        coEvery { repo.setActiveRole(any()) } returns Unit
        coEvery { repo.deleteCustom(any()) } returns Unit
        coEvery { repo.updateCustom(any(), any(), any(), any()) } returns Unit
        coEvery { repo.addCustom(any(), any(), any(), any()) } answers {
            Persona(
                id = "custom.fakeId",
                name = firstArg(),
                tagline = secondArg(),
                systemPromptText = thirdArg(),
                isBuiltIn = false,
                kind = arg(3),
            )
        }
        return repo
    }

    // ── initial state ────────────────────────────────────────────────

    test("initial state mirrors repository.value snapshots") {
        val initialCustom = Persona(
            id = "custom.x",
            name = "Pirate",
            tagline = "Arr",
            systemPromptText = "Talk like a pirate.",
            kind = PersonaKind.Voice,
        )
        val repo = fakeRepo(
            activeVoice = BuiltInPersonas.Editor,
            activeRole = BuiltInPersonas.Doctor,
            customPersonas = listOf(initialCustom),
        )

        val store = PersonasStore(repo)

        store.state.value shouldBe PersonasState(
            activeVoice = BuiltInPersonas.Editor,
            activeRole = BuiltInPersonas.Doctor,
            customPersonas = listOf(initialCustom),
        )
    }

    test("default initial state when repo flows have no overrides") {
        val store = PersonasStore(fakeRepo())

        store.state.value shouldBe PersonasState(
            activeVoice = BuiltInPersonas.Default,
            activeRole = null,
            customPersonas = emptyList(),
        )
    }

    // ── flow subscriptions ───────────────────────────────────────────

    test("state updates when repo.activeVoice emits a new value") {
        runTest {
            val voiceFlow = MutableStateFlow<Persona>(BuiltInPersonas.Default)
            val repo = fakeRepo().apply { every { activeVoice } returns voiceFlow }

            val store = PersonasStore(repo)
            advanceUntilIdle() // let init { collect } launch

            voiceFlow.value = BuiltInPersonas.FieldNotes
            advanceUntilIdle()

            store.state.value.activeVoice shouldBe BuiltInPersonas.FieldNotes
        }
    }

    test("state updates when repo.activeRole emits null → role transition") {
        runTest {
            val roleFlow = MutableStateFlow<Persona?>(null)
            val repo = fakeRepo().apply { every { activeRole } returns roleFlow }

            val store = PersonasStore(repo)
            advanceUntilIdle()

            roleFlow.value = BuiltInPersonas.Developer
            advanceUntilIdle()
            store.state.value.activeRole shouldBe BuiltInPersonas.Developer

            roleFlow.value = null
            advanceUntilIdle()
            store.state.value.activeRole shouldBe null
        }
    }

    test("state updates when repo.customPersonas emits a new list") {
        runTest {
            val customsFlow = MutableStateFlow<List<Persona>>(emptyList())
            val repo = fakeRepo().apply { every { customPersonas } returns customsFlow }

            val store = PersonasStore(repo)
            advanceUntilIdle()

            val newCustom = Persona(
                id = "custom.new",
                name = "Sailor",
                tagline = "Yo ho",
                systemPromptText = "Speak like a sailor.",
                kind = PersonaKind.Custom,
            )
            customsFlow.value = listOf(newCustom)
            advanceUntilIdle()

            store.state.value.customPersonas shouldBe listOf(newCustom)
        }
    }

    // ── TapPersona ───────────────────────────────────────────────────

    test("TapPersona on a Voice → setActiveVoice(id)") {
        runTest {
            val repo = fakeRepo()
            val store = PersonasStore(repo)

            store.dispatch(PersonasIntent.TapPersona(BuiltInPersonas.Editor))
            advanceUntilIdle()

            coVerify(exactly = 1) { repo.setActiveVoice(BuiltInPersonas.Editor.id) }
            coVerify(exactly = 0) { repo.setActiveRole(any()) }
        }
    }

    test("TapPersona on a Custom → setActiveVoice(id)") {
        runTest {
            val customVoice = Persona(
                id = "custom.cv",
                name = "Sci-fi",
                tagline = "Robotic",
                systemPromptText = "Speak in a robotic, factual tone.",
                kind = PersonaKind.Custom,
            )
            val repo = fakeRepo()
            val store = PersonasStore(repo)

            store.dispatch(PersonasIntent.TapPersona(customVoice))
            advanceUntilIdle()

            coVerify(exactly = 1) { repo.setActiveVoice(customVoice.id) }
            coVerify(exactly = 0) { repo.setActiveRole(any()) }
        }
    }

    test("TapPersona on a Role when role slot empty → setActiveRole(id)") {
        runTest {
            val repo = fakeRepo(activeRole = null)
            val store = PersonasStore(repo)

            store.dispatch(PersonasIntent.TapPersona(BuiltInPersonas.Doctor))
            advanceUntilIdle()

            coVerify(exactly = 1) { repo.setActiveRole(BuiltInPersonas.Doctor.id) }
            coVerify(exactly = 0) { repo.setActiveVoice(any()) }
        }
    }

    test("TapPersona on the active Role → setActiveRole(null) — toggle off") {
        runTest {
            // Active role already set to Doctor; tapping Doctor again clears it.
            val repo = fakeRepo(activeRole = BuiltInPersonas.Doctor)
            val store = PersonasStore(repo)

            store.dispatch(PersonasIntent.TapPersona(BuiltInPersonas.Doctor))
            advanceUntilIdle()

            coVerify(exactly = 1) { repo.setActiveRole(null) }
        }
    }

    test("TapPersona on a different Role swaps — setActiveRole(newId)") {
        runTest {
            val repo = fakeRepo(activeRole = BuiltInPersonas.Doctor)
            val store = PersonasStore(repo)

            store.dispatch(PersonasIntent.TapPersona(BuiltInPersonas.Lawyer))
            advanceUntilIdle()

            coVerify(exactly = 1) { repo.setActiveRole(BuiltInPersonas.Lawyer.id) }
        }
    }

    // ── AddCustom ────────────────────────────────────────────────────

    test("AddCustom(Voice) → addCustom then setActiveVoice(newId)") {
        runTest {
            val repo = fakeRepo()
            val store = PersonasStore(repo)

            store.dispatch(
                PersonasIntent.AddCustom(
                    name = "Pirate",
                    tagline = "Arr",
                    systemPromptText = "Talk like a pirate.",
                    kind = PersonaKind.Voice,
                ),
            )
            advanceUntilIdle()

            coVerifyOrder {
                repo.addCustom("Pirate", "Arr", "Talk like a pirate.", PersonaKind.Voice)
                repo.setActiveVoice("custom.fakeId")
            }
            coVerify(exactly = 0) { repo.setActiveRole(any()) }
        }
    }

    test("AddCustom(Role) → addCustom then setActiveRole(newId)") {
        runTest {
            val repo = fakeRepo()
            val store = PersonasStore(repo)

            store.dispatch(
                PersonasIntent.AddCustom(
                    name = "Mentor",
                    tagline = "Wise",
                    systemPromptText = "Mentor.",
                    kind = PersonaKind.Role,
                ),
            )
            advanceUntilIdle()

            coVerifyOrder {
                repo.addCustom("Mentor", "Wise", "Mentor.", PersonaKind.Role)
                repo.setActiveRole("custom.fakeId")
            }
            coVerify(exactly = 0) { repo.setActiveVoice(any()) }
        }
    }

    test("AddCustom(Custom) → addCustom then setActiveVoice(newId)") {
        runTest {
            // Custom kind currently routes through the voice slot (matches
            // the production code's `Voice, Custom -> setActiveVoice`).
            val repo = fakeRepo()
            val store = PersonasStore(repo)

            store.dispatch(
                PersonasIntent.AddCustom(
                    name = "Other",
                    tagline = "Mixed",
                    systemPromptText = "Mixed-purpose persona.",
                    kind = PersonaKind.Custom,
                ),
            )
            advanceUntilIdle()

            coVerify { repo.setActiveVoice("custom.fakeId") }
            coVerify(exactly = 0) { repo.setActiveRole(any()) }
        }
    }

    // ── UpdateCustom ─────────────────────────────────────────────────

    test("UpdateCustom calls repo.updateCustom with the supplied fields") {
        runTest {
            val repo = fakeRepo()
            val store = PersonasStore(repo)

            store.dispatch(
                PersonasIntent.UpdateCustom(
                    id = "custom.editme",
                    name = "Edited",
                    tagline = "Now edited",
                    systemPromptText = "Edited prompt text.",
                ),
            )
            advanceUntilIdle()

            coVerify(exactly = 1) {
                repo.updateCustom(
                    id = "custom.editme",
                    name = "Edited",
                    tagline = "Now edited",
                    systemPromptText = "Edited prompt text.",
                )
            }
            // No active-slot change on edit.
            coVerify(exactly = 0) { repo.setActiveVoice(any()) }
            coVerify(exactly = 0) { repo.setActiveRole(any()) }
        }
    }

    // ── DeleteCustom ─────────────────────────────────────────────────

    test("DeleteCustom forwards id to repo.deleteCustom") {
        runTest {
            val repo = fakeRepo()
            val store = PersonasStore(repo)

            store.dispatch(PersonasIntent.DeleteCustom("custom.deleteme"))
            advanceUntilIdle()

            coVerify(exactly = 1) { repo.deleteCustom("custom.deleteme") }
        }
    }

    // ── interaction discipline ───────────────────────────────────────

    test("a single intent does not invoke unrelated repository methods") {
        runTest {
            val repo = fakeRepo()
            val store = PersonasStore(repo)

            store.dispatch(PersonasIntent.TapPersona(BuiltInPersonas.Editor))
            advanceUntilIdle()

            // Asserting only setActiveVoice fires for this intent; the
            // explicit `exactly = 0` checks below would surface any
            // regression that accidentally double-fires a sibling
            // method (e.g. a future bug where TapPersona on a Voice
            // also clears the role). We deliberately don't reach for
            // `confirmVerified(repo)` here — the store's init block
            // legitimately reads `activeVoice` / `activeRole` /
            // `customPersonas` getters to seed its flow collectors, and
            // those reads would falsely trip confirmVerified.
            coVerify(exactly = 1) { repo.setActiveVoice(any()) }
            coVerify(exactly = 0) { repo.setActiveRole(any()) }
            coVerify(exactly = 0) { repo.addCustom(any(), any(), any(), any()) }
            coVerify(exactly = 0) { repo.updateCustom(any(), any(), any(), any()) }
            coVerify(exactly = 0) { repo.deleteCustom(any()) }
        }
    }
})

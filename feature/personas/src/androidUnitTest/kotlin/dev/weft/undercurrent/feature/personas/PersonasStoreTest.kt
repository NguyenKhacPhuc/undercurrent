package dev.weft.undercurrent.feature.personas

import dev.weft.undercurrent.core.model.BuiltInPersonas
import dev.weft.undercurrent.core.model.Persona
import dev.weft.undercurrent.core.model.PersonaKind
import dev.weft.undercurrent.data.datastore.PersonaRepository
import io.kotest.core.spec.style.BehaviorSpec
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
 * Exercises [PersonasStore] in BDD style.
 *
 * The repo is mocked via MockK; the three flow properties
 * (`activeVoice`, `activeRole`, `customPersonas`) are backed by
 * `MutableStateFlow`s so the spec can seed initial values and emit
 * changes mid-test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PersonasStoreTest : BehaviorSpec({

    val mainDispatcher = StandardTestDispatcher()
    beforeTest { Dispatchers.setMain(mainDispatcher) }
    afterTest { Dispatchers.resetMain() }

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

    Given("a repo seeded with Editor + Doctor + a single Pirate custom Voice") {
        val customPirate = Persona(
            id = "custom.x",
            name = "Pirate",
            tagline = "Arr",
            systemPromptText = "Talk like a pirate.",
            kind = PersonaKind.Voice,
        )
        val store = PersonasStore(
            fakeRepo(
                activeVoice = BuiltInPersonas.Editor,
                activeRole = BuiltInPersonas.Doctor,
                customPersonas = listOf(customPirate),
            ),
        )

        Then("the initial state mirrors all three flow snapshots") {
            store.state.value shouldBe PersonasState(
                activeVoice = BuiltInPersonas.Editor,
                activeRole = BuiltInPersonas.Doctor,
                customPersonas = listOf(customPirate),
            )
        }
    }

    Given("a repo with the default voice, no role, and no custom personas") {
        val store = PersonasStore(fakeRepo())

        Then("the initial state has the BuiltInPersonas defaults") {
            store.state.value shouldBe PersonasState(
                activeVoice = BuiltInPersonas.Default,
                activeRole = null,
                customPersonas = emptyList(),
            )
        }
    }

    Given("a store subscribed to a mutable activeVoice flow") {
        Then("the store reflects each new voice emission") {
            runTest {
                val voiceFlow = MutableStateFlow<Persona>(BuiltInPersonas.Default)
                val repo = fakeRepo().apply { every { activeVoice } returns voiceFlow }
                val store = PersonasStore(repo)
                advanceUntilIdle()

                voiceFlow.value = BuiltInPersonas.FieldNotes
                advanceUntilIdle()

                store.state.value.activeVoice shouldBe BuiltInPersonas.FieldNotes
            }
        }
    }

    Given("a store subscribed to a mutable activeRole flow") {
        Then("role transitions including null clears propagate into state") {
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
    }

    Given("a store subscribed to a mutable customPersonas flow") {
        Then("a new custom persona emission lands in state") {
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
    }

    Given("a fresh store with a stubbed repo") {
        When("TapPersona on a built-in Voice (Editor) is dispatched") {
            Then("repo.setActiveVoice(editor.id) is called once and setActiveRole is not") {
                runTest {
                    val repo = fakeRepo()
                    val store = PersonasStore(repo)

                    store.dispatch(PersonasIntent.TapPersona(BuiltInPersonas.Editor))
                    advanceUntilIdle()

                    coVerify(exactly = 1) { repo.setActiveVoice(BuiltInPersonas.Editor.id) }
                    coVerify(exactly = 0) { repo.setActiveRole(any()) }
                }
            }
        }

        When("TapPersona on a Custom-kind persona is dispatched") {
            Then("the custom routes through the voice slot — setActiveVoice with its id") {
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
        }

        When("TapPersona on a Role is dispatched and the role slot is empty") {
            Then("repo.setActiveRole(role.id) is called and setActiveVoice is not") {
                runTest {
                    val repo = fakeRepo(activeRole = null)
                    val store = PersonasStore(repo)

                    store.dispatch(PersonasIntent.TapPersona(BuiltInPersonas.Doctor))
                    advanceUntilIdle()

                    coVerify(exactly = 1) { repo.setActiveRole(BuiltInPersonas.Doctor.id) }
                    coVerify(exactly = 0) { repo.setActiveVoice(any()) }
                }
            }
        }
    }

    Given("a store with Doctor as the active role") {
        When("TapPersona(Doctor) is dispatched — re-tapping the active role") {
            Then("repo.setActiveRole(null) is called — toggle-off semantics") {
                runTest {
                    val repo = fakeRepo(activeRole = BuiltInPersonas.Doctor)
                    val store = PersonasStore(repo)

                    store.dispatch(PersonasIntent.TapPersona(BuiltInPersonas.Doctor))
                    advanceUntilIdle()

                    coVerify(exactly = 1) { repo.setActiveRole(null) }
                }
            }
        }

        When("TapPersona(Lawyer) is dispatched — swapping to a different role") {
            Then("repo.setActiveRole(lawyer.id) is called") {
                runTest {
                    val repo = fakeRepo(activeRole = BuiltInPersonas.Doctor)
                    val store = PersonasStore(repo)

                    store.dispatch(PersonasIntent.TapPersona(BuiltInPersonas.Lawyer))
                    advanceUntilIdle()

                    coVerify(exactly = 1) { repo.setActiveRole(BuiltInPersonas.Lawyer.id) }
                }
            }
        }
    }

    Given("a fresh store and the user adding a custom persona") {
        When("AddCustom(kind=Voice) is dispatched") {
            Then("addCustom runs, then setActiveVoice is set to the new id") {
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
        }

        When("AddCustom(kind=Role) is dispatched") {
            Then("addCustom runs, then setActiveRole is set to the new id") {
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
        }

        When("AddCustom(kind=Custom) is dispatched — the mixed bucket") {
            Then("the new custom routes through the voice slot") {
                runTest {
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
        }
    }

    Given("a fresh store editing an existing custom persona") {
        When("UpdateCustom(id, name, tagline, systemPromptText) is dispatched") {
            Then("repo.updateCustom is called with the four fields and no slot change") {
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
                    coVerify(exactly = 0) { repo.setActiveVoice(any()) }
                    coVerify(exactly = 0) { repo.setActiveRole(any()) }
                }
            }
        }
    }

    Given("a fresh store and the user deleting a custom persona") {
        When("DeleteCustom('custom.deleteme') is dispatched") {
            Then("repo.deleteCustom is called with that id") {
                runTest {
                    val repo = fakeRepo()
                    val store = PersonasStore(repo)

                    store.dispatch(PersonasIntent.DeleteCustom("custom.deleteme"))
                    advanceUntilIdle()

                    coVerify(exactly = 1) { repo.deleteCustom("custom.deleteme") }
                }
            }
        }
    }

    Given("a single TapPersona dispatched against a fresh store") {
        Then("only setActiveVoice fires — none of the other repo methods are touched") {
            runTest {
                val repo = fakeRepo()
                val store = PersonasStore(repo)

                store.dispatch(PersonasIntent.TapPersona(BuiltInPersonas.Editor))
                advanceUntilIdle()

                // We deliberately don't reach for `confirmVerified(repo)`
                // here — the store's init block legitimately reads
                // `activeVoice` / `activeRole` / `customPersonas` getters
                // to seed its flow collectors, and those reads would
                // falsely trip confirmVerified.
                coVerify(exactly = 1) { repo.setActiveVoice(any()) }
                coVerify(exactly = 0) { repo.setActiveRole(any()) }
                coVerify(exactly = 0) { repo.addCustom(any(), any(), any(), any()) }
                coVerify(exactly = 0) { repo.updateCustom(any(), any(), any(), any()) }
                coVerify(exactly = 0) { repo.deleteCustom(any()) }
            }
        }
    }
})

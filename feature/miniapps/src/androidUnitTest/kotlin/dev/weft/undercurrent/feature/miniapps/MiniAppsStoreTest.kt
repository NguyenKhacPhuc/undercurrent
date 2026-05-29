package dev.weft.undercurrent.feature.miniapps

import dev.weft.undercurrent.core.model.MiniApp
import dev.weft.undercurrent.data.datastore.MiniAppsRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
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
 * Exercises [MiniAppsStore]. The repo is mocked via MockK; its
 * `miniApps` StateFlow is backed by a `MutableStateFlow` so the test
 * can both seed initial state and emit changes mid-test.
 *
 * Each intent (Add, Update, Delete) translates to a single suspend
 * call on the repo. The store doesn't optimistically update local
 * state — it waits for the repo's StateFlow to re-emit.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MiniAppsStoreTest : FunSpec({

    val mainDispatcher = StandardTestDispatcher()

    beforeTest { Dispatchers.setMain(mainDispatcher) }
    afterTest { Dispatchers.resetMain() }

    fun miniApp(
        id: String,
        name: String = "App $id",
        emoji: String = "🦋",
        prompt: String = "do $id",
    ): MiniApp = MiniApp(
        id = id,
        name = name,
        emoji = emoji,
        triggerPrompt = prompt,
        createdAtEpochMs = 0L,
    )

    fun fakeRepo(initial: List<MiniApp> = emptyList()): MiniAppsRepository {
        val repo = mockk<MiniAppsRepository>()
        every { repo.miniApps } returns MutableStateFlow(initial)
        coEvery { repo.add(any(), any(), any()) } returns miniApp("added")
        coEvery { repo.update(any(), any(), any(), any()) } returns Unit
        coEvery { repo.delete(any()) } returns Unit
        return repo
    }

    // ── initial state ────────────────────────────────────────────────

    test("initial state mirrors repo.miniApps.value snapshot") {
        val seed = listOf(miniApp("a"), miniApp("b"))
        val repo = fakeRepo(initial = seed)

        val store = MiniAppsStore(repo)

        store.state.value shouldBe MiniAppsState(miniApps = seed)
    }

    test("initial state is empty when repo has no mini-apps") {
        val store = MiniAppsStore(fakeRepo())

        store.state.value shouldBe MiniAppsState(miniApps = emptyList())
    }

    // ── live flow ────────────────────────────────────────────────────

    test("state updates when repo.miniApps emits a new list") {
        runTest {
            val flow = MutableStateFlow<List<MiniApp>>(emptyList())
            val repo = mockk<MiniAppsRepository>().apply {
                every { miniApps } returns flow
                coEvery { add(any(), any(), any()) } returns miniApp("added")
                coEvery { update(any(), any(), any(), any()) } returns Unit
                coEvery { delete(any()) } returns Unit
            }

            val store = MiniAppsStore(repo)
            advanceUntilIdle()

            flow.value = listOf(miniApp("after"))
            advanceUntilIdle()

            store.state.value.miniApps shouldBe listOf(miniApp("after"))
        }
    }

    // ── Add ──────────────────────────────────────────────────────────

    test("Add forwards (name, emoji, triggerPrompt) to repo.add") {
        runTest {
            val repo = fakeRepo()
            val store = MiniAppsStore(repo)

            store.dispatch(
                MiniAppsIntent.Add(
                    name = "Daily standup",
                    emoji = "📝",
                    triggerPrompt = "Generate today's standup notes",
                ),
            )
            advanceUntilIdle()

            coVerify(exactly = 1) {
                repo.add("Daily standup", "📝", "Generate today's standup notes")
            }
            coVerify(exactly = 0) { repo.update(any(), any(), any(), any()) }
            coVerify(exactly = 0) { repo.delete(any()) }
        }
    }

    // ── Update ───────────────────────────────────────────────────────

    test("Update forwards (id, name, emoji, triggerPrompt) to repo.update") {
        runTest {
            val repo = fakeRepo()
            val store = MiniAppsStore(repo)

            store.dispatch(
                MiniAppsIntent.Update(
                    id = "feature.legacy-id",
                    name = "Renamed",
                    emoji = "✏️",
                    triggerPrompt = "Updated prompt",
                ),
            )
            advanceUntilIdle()

            coVerify(exactly = 1) {
                repo.update("feature.legacy-id", "Renamed", "✏️", "Updated prompt")
            }
            coVerify(exactly = 0) { repo.add(any(), any(), any()) }
            coVerify(exactly = 0) { repo.delete(any()) }
        }
    }

    // ── Delete ───────────────────────────────────────────────────────

    test("Delete forwards id to repo.delete") {
        runTest {
            val repo = fakeRepo()
            val store = MiniAppsStore(repo)

            store.dispatch(MiniAppsIntent.Delete("miniapp-42"))
            advanceUntilIdle()

            coVerify(exactly = 1) { repo.delete("miniapp-42") }
            coVerify(exactly = 0) { repo.add(any(), any(), any()) }
            coVerify(exactly = 0) { repo.update(any(), any(), any(), any()) }
        }
    }

    // ── interaction discipline ───────────────────────────────────────

    test("mixing Add, Update, Delete fires each method exactly once") {
        runTest {
            val repo = fakeRepo()
            val store = MiniAppsStore(repo)

            store.dispatch(MiniAppsIntent.Add("Cat", "🐈", "do cat"))
            store.dispatch(MiniAppsIntent.Update("id1", "Dog", "🐕", "do dog"))
            store.dispatch(MiniAppsIntent.Delete("id2"))
            advanceUntilIdle()

            coVerify(exactly = 1) { repo.add("Cat", "🐈", "do cat") }
            coVerify(exactly = 1) { repo.update("id1", "Dog", "🐕", "do dog") }
            coVerify(exactly = 1) { repo.delete("id2") }
        }
    }

    test("Update does not optimistically mutate local state") {
        // The store doesn't apply the patch locally; it waits for repo
        // to re-emit the new list. Without a gateway-side StateFlow
        // tick, state remains the prior snapshot.
        runTest {
            val seed = listOf(miniApp("solo", name = "Original"))
            val repo = fakeRepo(initial = seed)
            val store = MiniAppsStore(repo)
            advanceUntilIdle()

            store.dispatch(
                MiniAppsIntent.Update("solo", "Renamed", "✏️", "new prompt"),
            )
            advanceUntilIdle()

            store.state.value.miniApps shouldBe seed
        }
    }
})

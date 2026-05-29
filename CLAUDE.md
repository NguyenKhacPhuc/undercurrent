# Undercurrent — Claude working notes

Auto-loaded as context by Claude Code. Keep terse — every session pays
for these tokens.

## What this repo is

Undercurrent is a personal-assistant Android app built on the Weft SDK.
Streaming chat, multi-provider key management, custom personas, memory
browsing, OAuth-gated integrations (Linear). It's also the reference
app for Weft — when something doesn't have an obvious pattern, this
is where to invent it first.

The Weft SDK is in a sibling directory: `../weft`. Wired via
`includeBuild("../weft")` in `settings.gradle.kts`. Both repos must be
cloned side-by-side for the build to resolve. Weft SDK is published as
<https://github.com/NguyenKhacPhuc/android-harness>.

## The architectural rule (read first)

> **The SDK provides everything. The app just registers.**

Full split documented in `../weft/docs/architecture-vision.md`. When
adding logic to this repo, ask: *would another Weft host need this
same behavior?* If yes, it goes in `../weft`, not here. The app's
responsibilities are: identity (preamble + theme), screens (the
Compose UI shell), DI wiring (Koin module), and *which* tools /
components / data-sources / MCP-servers / integrations to register.
The data-binding system, `$exec` action interception, `$binding`
resolution, prompt assembly, memory tools, etc. all live in the SDK
and are exposed via registration hooks. Don't duplicate substrate
behavior into the app — fix it in the SDK and let it flow back.

## Build & test

```bash
# Debug APK (NEW: the entrypoint module is :androidApp, not :app).
./gradlew :androidApp:assembleDebug

# Just the Kotlin compile (faster when iterating).
./gradlew :androidApp:compileDebugKotlin

# A single feature module — verifies BOTH Android + iOS compile
# (catches features that accidentally touch Android-only APIs from
# commonMain).
./gradlew :feature:chat:compileKotlinAndroid
./gradlew :feature:chat:compileKotlinIosSimulatorArm64

# Install + start + tail logs.
adb shell am force-stop dev.weft.undercurrent
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb shell am start -n dev.weft.undercurrent/.MainActivity
adb logcat | grep -E "Undercurrent|YourTag"

# Unit tests — one feature module
./gradlew :feature:personas:testDebugUnitTest

# Unit tests — all modules (`testDebugUnitTest` is the conventional
# task name from the AGP test plugin).
./gradlew testDebugUnitTest
```

## Module layout (KMP)

```
undercurrent/
├── androidApp/          Android entry — Application + MainActivity
├── composeApp/          CMP shared UI (commonMain + androidMain + iosMain)
├── iosApp/              iOS Xcode project (embeds ComposeApp.framework)
├── shared/              KMP business interfaces (AgentEngine, etc.)
├── core/                model, ui, design-system, navigation, resources, domain, ext
├── data/                repository, datastore, sqldelight, network, weft (Android-only)
├── feature/             one Gradle module per screen/flow (17 features)
├── build-logic/         convention plugins (undercurrent.kmp.feature etc.)
└── app/                 OLD single-module — being emptied into the above
```

**Weft stays Android-only.** Feature modules MUST NOT depend on
Weft directly (would break iOS compilation). Weft access goes
through `:data:weft` which exposes Android implementations of
KMP-shared interfaces declared in `:shared`.

See [`docs/kmp-migration-playbook.md`](docs/kmp-migration-playbook.md)
for the file-by-file migration recipe.

**Force-stop is important** when iterating on tools. The
`WeftRuntime`'s tool catalog is built once at process start; reinstalling
without killing the old process means the agent keeps using the stale
catalog.

## Architecture

MVI everywhere — both the orchestrator (AppStore) and per-feature
screens follow the same shape. Koin for DI.

### MVI pattern (load-bearing — every new feature follows this)

The generic base lives at
`:shared/src/commonMain/kotlin/dev/weft/undercurrent/shared/mvi/Store.kt`:

```kotlin
abstract class Store<State, Intent, Effect>(initialState: State) : ViewModel() {
    val state: StateFlow<State>      // observable; Compose collectAsState
    val effects: Flow<Effect>        // one-shot; LaunchedEffect collect
    protected val current: State     // current snapshot inside handlers
    abstract fun dispatch(intent: Intent)
    protected fun update(reducer: (State) -> State)
    protected fun emit(effect: Effect)
}
```

Every feature has the same four files:

```
:feature:<name>/src/commonMain/kotlin/.../feature/<name>/
├─ <Name>Mvi.kt        ← State (data class) + Intent + Effect (sealed)
├─ <Name>Store.kt      ← class <Name>Store(...) : Store<State, Intent, Effect>(...)
├─ <Name>Screen.kt     ← @Composable; reads state.collectAsState(), dispatches intents
└─ (optional helpers)
```

Conventions:
- **`<Name>State`** is a single immutable data class. No nullable
  StateFlows; aggregate everything one screen needs into one bundle.
- **`<Name>Intent`** is a sealed interface. One variant per user action;
  data-class variants carry params. Read access doesn't go through
  intents — only mutations do.
- **`<Name>Effect`** is a sealed interface, often empty at first.
  Reserve for things that genuinely shouldn't replay on configuration
  change (toasts, transient navigation hints). State is for everything
  else.
- **Subscribe to repos / gateways in `init`.** The store is the
  projection point: collect repo flows and project into your state via
  `update { it.copy(foo = ...) }`. Don't expose repo flows directly.
- **Plain helpers are OK for pure projections** — e.g.
  `IntegrationsStore.statusFor(integration, enabled): IntegrationStatus`
  is a pure function, not a mutation, so it doesn't go through dispatch.
- **Screens never call `viewModelScope` directly.** They dispatch;
  the store launches coroutines.
- **`koinViewModel<XxxStore>()`** — use
  `org.koin.compose.viewmodel.koinViewModel` (the KMP one), not the
  Android-only `org.koin.androidx.compose.koinViewModel`.

### The orchestrator (root AppStore)

The same pattern applies, just at a wider scope:

- **Interface** `dev.weft.undercurrent.app.AppStore` lives in
  `:composeApp/commonMain/.../app/AppStore.kt`. Both platforms see
  the same surface (`state` / `effects` / `displayMessages` /
  `skills` / `dispatch` / `sendUiEvent` / `saveKey`).
- **Android impl** `WeftAppStore` in
  `:androidApp/.../core/WeftAppStore.kt` —
  `: Store<AppState, AppIntent, AppEffect>(AppState.initial()), AppStore`.
  Closes over `WeftRuntime` directly; full agent loop here.
- **iOS impl** `IosAppStore` in `:composeApp/iosMain/.../app/IosAppStore.kt`
  — same shape, backed by the Ktor `LlmClient` stack +
  `KeychainKeyVaultGateway` + DataStore-Preferences + SQLDelight.

State types ([`AppState`](composeApp/src/commonMain/kotlin/dev/weft/undercurrent/app/AppState.kt) /
[`AppIntent`](composeApp/src/commonMain/kotlin/dev/weft/undercurrent/app/AppIntent.kt) /
`AppEffect`) are commonMain mirrors — feature screens that consume the
app store get types they can read on both platforms without dragging
the substrate in.

Per-screen surfaces are wired through `ScreenRouter`:
- 11 KMP-clean screens render inline (Settings / Appearance / KeyPaste
  / Onboarding / Providers / Personas / Memories / Traces / Usage /
  Integrations / Conversations).
- 4 substrate-coupled screens go through `PlatformAdapter` lambdas
  (Chat / RenderedTree / Creator / MiniApps with `TreeRenderer`).

Adding a new screen:
1. Create the `:feature:<name>/` module via the convention plugin.
2. Add the four files (Mvi / Store / Screen / + the build.gradle.kts
   feature/data deps).
3. Wire Koin in `:androidApp/.../di/AppModule.kt` and
   `:composeApp/iosMain/.../app/IosKoinModule.kt`:
   `viewModel { XxxStore(...) }`.
4. Add a `Screen.<Name>` variant in `:core:navigation` if it's
   navigable.
5. Add a `ScreenRouter` branch in
   `:composeApp/commonMain/.../app/ScreenRouter.kt`.

### Other architectural notes

- **`di/AppModule.kt`** (Android) + **`IosKoinModule.kt`** (iOS) are
  the **single source of truth** for which tools the agent has, which
  OAuth tokens exist, which MCP servers are wired up, which gateways
  bind to which impls.
- **`MainActivity.kt`** — Compose host. Renders `App()` from
  `:composeApp/commonMain`. Owns OAuth deep link
  (`undercurrent://oauth/…`) and the process restart for MCP changes.
- **`features/<feature>/`** — legacy phrasing; today every feature is
  its own KMP module under `:feature:<name>/`. Each has a Store, a
  Screen, optionally a Repository (in `:data:datastore`), and
  optionally agent tools (in `:data:weft/.../tools/` on Android since
  Weft tools must be androidMain).

## Tool-authoring rules (do NOT skip)

Adding agent tools is a recurring task in this repo. Tool selection is
a soft attention process — names + descriptions determine whether the
model picks the tool or silently skips it.

Full guide: `../weft/docs/writing-a-custom-tool.md`. Headlines:

1. **Name: `<verb>_<noun>`, ≤3 words, lowercase_snake_case.**
   `open_map`, `set_theme_palette`, `open_personas`. Compound names
   with prepositions (`show_location_on_map`) get skipped.
2. **Description: lead with the action.** "Open the map app pinned
   at…" beats "This tool is used when…". Include 2–4 user-phrasing
   examples.
3. **Cap descriptions at ~3 sentences.** Long descriptions cause
   tool-skip behavior.
4. **Disambiguate from neighbors.** When two tools sound similar, add
   a sentence like "NOT for directions — use `other_tool` for X."
5. **Use `KotlinTypeToken(typeOf<Args>())`, not `typeToken<Args>()`.**
   The app builds JVM 11; substrate's inline `typeToken` is JVM 17
   bytecode and won't inline.

When a tool gets narrated but not called, the cure is usually (in
order): force-stop + reinstall, fresh conversation, rename to
verb-noun shape, shorten description.

## App-specific patterns to know

- **Agent tools live in `features/<area>/`.** Theme tools in
  `features/theme/`, navigation tools in `features/navigation/`, map
  tool in `features/maps/`. Wire them in `di/AppModule.kt` via
  `extraToolsFactory`.
- **`NavigationChannel`** is how tools navigate the user. The agent
  fires `open_personas` → tool emits to the channel → `AppStore`
  collects + dispatches `AppIntent.Navigate`. Keeps `previousScreen`
  correct.
- **Permission denials surface as a dialog.** `AppStore` detects
  `PermissionDeniedException` shape in `StreamChunk.ToolFailed`,
  populates `pendingPermissionDialog`, and MainActivity renders an
  AlertDialog with an "Open Settings" deep-link. Don't let
  permission errors land in chat as toolFail bubbles.
- **MCP integrations need a process restart.** `WeftRuntime` bakes
  the MCP server list in at construction. Connect/Disconnect in the
  Integrations screen persists the change, then prompts the user to
  restart. `restartProcess(context)` in `MainActivity.kt` does the
  kill-and-relaunch.
- **Anti-hallucination preamble.** `core/AppPreamble.kt` includes an
  explicit "never narrate a tool call you don't make" directive.
  Keep it — without it, Claude sometimes describes tool calls in
  text without emitting the tool_use block.

## Testing (MVI stores)

Unit-testing rules for feature stores. The harness is wired in
`KmpLibraryConventionPlugin` — MockK + Kotest + Turbine + coroutines-test
are auto-added to every KMP library's `androidUnitTest` source set; no
per-module build changes needed.

**Stack — split across `commonTest` and `androidUnitTest`.**

- **commonTest** (runs on Android + iOS): Kotest `BehaviorSpec` engine
  + Kotest matchers + Turbine + kotlinx-coroutines-test +
  `kotlin.test`. Tests here use **hand-rolled fakes** for
  collaborators — MockK is JVM-only.
- **androidUnitTest** (JVM only, layered on top of commonTest): adds
  `kotest-runner-junit5` (so JUnit Platform discovers the specs) and
  **MockK**. Use these for collaborator-interaction tests where
  `coVerify` / `coVerifyOrder` earns its keep.

The convention plugin wires both source sets automatically. Per-feature
`build.gradle.kts` files don't touch test deps.

**BDD style.** Outer `Given` describes the setup precondition (a fresh
store, a seeded gateway, a specific flow shape), inner `When`
describes the intent dispatched (or event simulated), innermost
`Then` describes the observable outcome. Use uppercase
`Given` / `When` / `Then` to avoid the `when` keyword backtick
collision. A `Given` with no `When` is fine for pure initial-state
projections (no action — just observe).

**Per-spec split convention.** For each MVI store, prefer two specs
with **NO overlap** between them:

  - `<Name>StoreStateTest.kt` in **commonTest** — covers initial state,
    live-flow propagation, "store didn't mutate optimistically" — any
    assertion that only reads `store.state.value` or observes via
    Turbine. Uses a hand-rolled `Fake<Gateway>` (KMP-portable). Runs
    on Android + iOS.
  - `<Name>StoreTest.kt` in **androidUnitTest** — **only** the Thens
    that need MockK's `coVerify(exactly = N) { gateway.method(args) }`
    to assert the right gateway method was called with the right
    arguments. State-shape assertions (`shouldBe …` on
    `store.state.value`) live in the commonTest sibling — do not
    duplicate them here.

The split is by *what an assertion checks*, not by *what the test
exercises*: a test that dispatches an Intent and asserts only the
new state belongs in commonTest; a test that dispatches and asserts
the gateway was called belongs in androidUnitTest.

The base `Store` class (`shared/.../mvi/StoreTest.kt`) and the
read-only `UsageStore` (one interface gateway, no dispatch) live
**entirely in commonTest** — they don't need MockK at all.

**Blocker: three feature stores can't fully split today.**
`MiniAppsStore`, `PersonasStore`, and `IntegrationsStore` each take a
concrete `final` repository (`MiniAppsRepository`, `PersonaRepository`,
`IntegrationsRepository`) — Kotlin classes, not interfaces, so a
commonTest fake can't subclass them. To split these, the production
code needs an interface extraction (rename current → `…Impl`, add an
interface with the public surface), or a commonTest fake has to wrap
the real class with an in-memory `DataStore<Preferences>` stand-in.
Neither is shipped yet; those three specs stay JVM-only via MockK.

**Source set.** Tests live under
`<module>/src/androidUnitTest/kotlin/<package>/<Class>Test.kt`. The
Android target's JVM runs them via the JUnit 5 runner. Do NOT put
JVM-only mocking in `commonTest` — MockK doesn't support Kotlin/Native.

**The 5 idioms every store test repeats:**

1. **Replace `Dispatchers.Main`.** `Store` extends `ViewModel`;
   `viewModelScope` dispatches on Main. Every spec needs:
   ```kotlin
   val mainDispatcher = StandardTestDispatcher()
   beforeTest { Dispatchers.setMain(mainDispatcher) }
   afterTest { Dispatchers.resetMain() }
   ```

2. **`fakeGateway(...)` helper.** Centralize the MockK setup — every
   StateFlow property returns a `MutableStateFlow(seed)`, every
   suspend method `coEvery { … } returns Unit`:
   ```kotlin
   fun fakeRepo(initial: List<Foo> = emptyList()): FooRepo {
       val repo = mockk<FooRepo>()
       every { repo.foos } returns MutableStateFlow(initial)
       coEvery { repo.add(any()) } returns Unit
       return repo
   }
   ```

3. **`runTest { … advanceUntilIdle() }`** for any test that calls
   `dispatch()`. The store does `viewModelScope.launch { repo.foo() }`
   internally — the suspend call doesn't fire until the test scheduler
   drains.

4. **Simulate live emissions via `flow.value = newValue`** then
   `advanceUntilIdle()`. The store's init-block collector picks up
   the change and projects it into state.

5. **`coVerify(exactly = N)` over `confirmVerified`.** The store's init
   block reads gateway flow getters (`activeVoice`, etc.) to seed the
   projection — those reads would falsely trip `confirmVerified`. Use
   per-method `coVerify(exactly = N)` calls instead; they catch the
   same regression class without the false positive.

**Coverage targets per store:** initial state from gateway snapshot,
live state updates on each flow emission, every Intent variant's
gateway call + resulting state/effect change, at least one
"interaction discipline" test (a single intent doesn't fire unrelated
methods — catches the regression-class where Tap accidentally fires
Add).

**What NOT to test in this layer:** transient state between
`dispatch()` and a suspend completion. MockK's `coEvery { … } returns
…` doesn't truly suspend, so `runCurrent()` advances through the
whole launched block in one step — the InProgress / Loading state is
gone before assertions run. Test the final state of each intent path
instead, and trust the `update { … }` calls in the middle.

**Reference templates.**

- `shared/.../mvi/StoreTest.kt` (**commonTest**) — generic `Store`
  base, no MockK, no fakes; test double is a tiny `CounterStore`.
- `feature/usage/.../UsageStoreTest.kt` (**commonTest**) — single
  interface gateway, hand-rolled `FakeUsageGateway`. Cleanest
  illustration of the KMP pattern.
- `feature/memories/.../MemoriesStoreStateTest.kt` (**commonTest**) +
  `MemoriesStoreTest.kt` (**androidUnitTest**) — the split pattern.
  State-projection in commonTest with a fake; intent forwarding in
  androidUnitTest with MockK `coVerify`.
- `feature/personas/.../PersonasStoreTest.kt` (**androidUnitTest**) —
  the all-MockK pattern for stores whose collaborator is a concrete
  final class.
- `feature/integrations/.../IntegrationsStoreTest.kt`
  (**androidUnitTest**) — sealed-result-handling reference
  (`OAuthResult` mapped to `ActionStatus.Failure` per variant).

## Provider + persona + theme

- `features/providers/` — Anthropic / OpenAI / OpenRouter / DeepSeek
  key management + per-tier model overrides. `KeyVault` (encrypted
  prefs) holds the keys.
- `features/personas/` — voice + role (two independent slots).
  Injected per turn via `extraVolatilePrefix` in the runtime config.
  No agent rebuild needed on switch.
- `theme/` — palette + light/dark mode. DataStore-backed. Set via
  agent tools (`set_theme_palette`, `set_theme_mode`) or via the
  "Add to Chat" sheet on the chat screen.

## Conventions

- **No emojis in code or commits** unless the user explicitly asks.
- **Internal-only by default.** `internal class FooViewModel`, not
  `class FooViewModel`. App code never gets consumed externally.
- **AppStore is the single source of truth.** ViewModels for specific
  surfaces (PersonasViewModel etc.) own their feature's persistence
  but route navigation + agent state through AppStore.
- **DataStore for prefs, KeyVault for secrets, SQLDelight (via Weft)
  for conversations + memory.** Don't mix.

## What NOT to do

- Don't add tools directly to the substrate (`../weft/tools/`); they
  belong here via `extraToolsFactory`.
- Don't change tool names without testing — the model is sensitive
  to wording.
- Don't bypass `AppStore` to mutate `AppState`. The reducer is the
  only writer; views observe.
- Don't reformat the AppPreamble casually. Each sentence has been
  tuned against agent behavior.
- Don't add screens that don't show up in the side drawer or
  Settings — the user has no way to reach them otherwise.
- Don't put MockK or kotest-runner-junit5 in `commonTest` — both
  are JVM-only. The convention plugin keeps them in
  `androidUnitTest`. Kotest's engine + assertions + Turbine
  themselves ARE KMP and DO live in `commonTest`.
- Don't reach for `confirmVerified(mock)` in store tests. The init
  block reads gateway flow getters to seed state; `confirmVerified`
  treats those as unverified calls and fails. Use per-method
  `coVerify(exactly = N)`.
- Don't try to assert transient `Loading` / `InProgress` state
  mid-coroutine with MockK + `runCurrent()`. MockK's `coEvery`
  returns immediately rather than suspending, so the dispatcher
  advances past the transient before the assertion runs. Test the
  terminal state of each path instead.

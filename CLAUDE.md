# Undercurrent — Claude working notes

Auto-loaded as context by Claude Code. Keep terse — every session pays
for these tokens.

## What this repo is

Undercurrent is a personal-assistant app (Android + iOS) built on the
Weft SDK. Streaming chat, multi-provider key management, custom
personas, memory browsing, OAuth-gated integrations. Also the reference
host for Weft — new patterns get invented here first.

Weft SDK lives at `../weft` and is wired via `includeBuild("../weft")`
in `settings.gradle.kts`. Both repos must be cloned side-by-side.

## The architectural rule (read first)

> **The SDK provides everything. The app just registers.**

The full split is documented in `../weft/docs/architecture-vision.md`.
Before adding logic, ask: *would another Weft host need this same
behavior?* If yes → SDK. If it's about this app's identity / UX /
branding → here.

## Module layout

```
undercurrent/
├── androidApp/         Android entry — Application + MainActivity
├── composeApp/         CMP shared UI shell (ScreenRouter, AppViewModel iOS impl)
├── iosApp/             iOS Xcode project (embeds ComposeApp.framework)
├── shared/             KMP — MviViewModel base + AgentState mirror
├── core/
│   ├── model           pure data (KMP)
│   ├── domain          repository interfaces + Weft impls (androidMain)
│   │                   + iOS stubs (iosMain) + 4 chat use cases + RepositoryModule
│   ├── navigation      Screen + NavigationViewModel + NavigationChannel
│   ├── ui              shared composables (LoadingPlaceholder, AppDrawer …)
│   ├── design-system   palette + typography + shapes
│   ├── resources       strings, drawables
│   └── ext             KMP extension helpers
├── data/
│   ├── datastore       KMP DataStore-Preferences factory + Koin
│   ├── sqldelight      schema + drivers + Koin
│   ├── network         Ktor client + token interceptor (when needed outside Weft)
│   └── weft            Android-only — Weft *tools* (NavigationTools, ShowLocationOnMapTool,
│                       CreatorTools). Repository impls moved to :core:domain/androidMain.
├── feature/            one Gradle module per screen/flow (~17 features)
│   └── <name>/         <Name>ViewModel + <Name>Screen + <Name>Route + <Name>Module
└── build-logic/        convention plugins (undercurrent.kmp.feature etc.)
```

**KMP discipline.** Feature modules MAY depend on **KMP-published**
substrate modules (`weft-runtime`, `weft-harness-*`, `weft-contracts`,
…) from `commonMain` — the substrate is KMP now, so this no longer
breaks iOS. The chat feature's `commonMain` owns the shared agent host
(`AgentSlot`, `WeftAgentFactory`, `AgentSession`). Still keep
Android-only substrate bits (e.g. the Koog model catalog under
`dev.weft.android.routing`) behind an `expect/actual` seam — see
`feature/chat/.../agent/ModelPoolOverride.kt` (iOS returns the runtime's
default pool).

**Repository impls live with the interface.** `:core:domain/commonMain`
holds every `*Repository` interface; `:core:domain/androidMain` holds
Weft-backed impls (`WeftKeyVaultRepository`, `AndroidSpeechRepository`,
…); `:core:domain/iosMain` holds Keychain / stub impls
(`KeychainKeyVaultRepository`, `StubUsageRepository`, …). Each platform
publishes its Koin bindings as `repositoryAndroidModule` /
`repositoryIosModule` (the latter inline in `IosKoinModule.kt`).

## MVI pattern (load-bearing)

Generic base at `:shared/src/commonMain/.../mvi/MviViewModel.kt`:

```kotlin
abstract class MviViewModel<State, Intent, Effect>(initialState: State) : ViewModel() {
    val state: StateFlow<State>
    val effects: Flow<Effect>
    protected val current: State
    abstract fun dispatch(intent: Intent): Job

    protected fun update(reducer: (State) -> State)
    protected fun emit(effect: Effect)

    // Sugar — every feature VM uses these instead of viewModelScope.launch:
    protected fun launch(block: suspend CoroutineScope.() -> Unit): Job
    protected fun <T> Flow<T>.collectInto(reducer: State.(T) -> State): Job
    protected fun <T> Flow<T>.observe(block: suspend (T) -> Unit): Job
}
```

**`dispatch` returns `Job`.** Every override uses the
`= launch { when {…} }` form — branches just call `suspend` functions
directly. Don't wrap individual branches in `launch { }` — the outer
`launch` already provides a coroutine context.

```kotlin
override fun dispatch(intent: ThemeIntent) = launch {
    when (intent) {
        is ThemeIntent.SetPalette -> repo.setPalette(intent.palette)
        is ThemeIntent.SetThemeMode -> repo.setMode(intent.mode)
    }
}
```

**`collectInto` for state projection.** Replaces the boilerplate
`viewModelScope.launch { flow.collect { update { it.copy(...) } } }`:

```kotlin
init {
    themeRepo.prefsFlow.collectInto { copy(themePrefs = it) }
    onboardingRepo.completedFlow.collectInto { copy(onboardingCompleted = it) }
}
```

The reducer's receiver is the current `State` — write `copy(...)`
directly, not `it.copy(...)`.

**`observe` for side-effects on a flow.** Use when the collector
needs to call beyond `update`.

## Per-feature module structure

```
:feature:<name>/src/commonMain/.../feature/<name>/
├── <Name>State.kt        (data class — one bundle the screen reads)
├── <Name>Intent.kt       (sealed interface — one variant per user action)
├── <Name>Effect.kt       (sealed interface — one-shot signals; often empty)
├── <Name>ViewModel.kt    (extends MviViewModel<S, I, E>)
├── <Name>Screen.kt       (stateless Composable: state in, callbacks out)
├── <Name>Route.kt        (stateful entry point — koinInjects + dispatches)
└── <Name>Module.kt       (Koin: single { <Name>ViewModel(...) })
```

**Route is the entry point.** `ScreenRouter` is a thin dispatch table:
each `Screen.X` branch calls `<Name>Route()`. The Route owns its own
VM injection + intent dispatch + state collection; ScreenRouter never
injects feature VMs.

**The Route injects ViewModels — never repositories.** A repository in
the view layer is the anti-pattern: the View must not read a
`*Repository` flow or call a repo method directly. If a screen needs
data or an action, it goes through a ViewModel — state out, `Intent`
in. The Route may `koinInject` ViewModels (+ `NavigationViewModel`)
and pass plain state + callbacks down. The *only* DI a Route does is
resolve VMs; it never resolves a `*Repository`, a `*Store`, or a
`DataStore`.

```kotlin
// :feature:settings/.../SettingsRoute.kt
@Composable
fun SettingsRoute() {
    val nav: NavigationViewModel = koinInject()
    val vm: SettingsViewModel = koinViewModel()
    val state by vm.state.collectAsState()
    SettingsScreen(
        activeProvider = state.activeProvider,
        onShowProvider = { nav.dispatch(NavigationIntent.Navigate(Screen.Providers)) },
        ...
    )
}
```

The owning VM holds the repository; the Route reads its `state`. Two
corollaries:

- When a screen pulls from several repos that aren't its core concern,
  put that bundle in its own VM rather than injecting the repos in the
  view or bloating the primary VM.
- When a VM is an interface with per-platform impls, build its `state`
  once in a shared `commonMain` producer both impls delegate to; let
  only the side-effecting `dispatch` differ.

Platform-specific callbacks (`onOpenConsole`, `onRestartProcess`) come
in as Route parameters — `ScreenRouter` passes them through from
`PlatformAdapter`. Platform-specific Routes take the platform bits as
params but still inject only VMs.

**Stateful / stateless screen split.** The Screen Composable takes
state + callbacks (testable + previewable). The Route is the stateful
wrapper. `@Preview` renders against the stateless Screen.

## UI rules (Compose)

1. **Under 300 lines per file.** When a Screen grows past that, split
   sub-Composables into a `components/` sibling subpackage
   (`feature/chat/components/ChatHeader.kt`, `MessageBlock.kt`, …).
   The top-level Screen / Route file stays a thin assembly.
2. **Every Composable file has at least one `@Preview`.** Use
   `org.jetbrains.compose.ui.tooling.preview.Preview` (the CMP-native
   annotation, usable from commonMain). The AndroidX
   `androidx.compose.ui.tooling.preview.Preview` is Android-only and
   forbidden in commonMain. Wrap the preview body in
   `UndercurrentTheme { … }` so it renders against real tokens. One
   realistic preview per Composable is the bar; add an "empty state"
   variant only when that UI is non-trivial.
3. **Stateless + stateful split (state hoisting).** Every screen has
   TWO Composables: the *stateless* one (state in, callbacks out — no
   `koinInject`, no `collectAsState`) and the *stateful* `Route`
   wrapper that does the injection + state collection. `@Preview`
   always renders the stateless one. Local UI scratch state
   (`remember { mutableStateOf }` for "which dialog is open",
   text-input drafts, expand toggles) stays inside the stateless
   Composable — those don't belong on the ViewModel.

```kotlin
// stateful — the entry point ScreenRouter calls
@Composable
fun XxxRoute() {
    val vm: XxxViewModel = koinInject()
    val state by vm.state.collectAsState()
    XxxScreen(
        state = state,
        onAction = { vm.dispatch(XxxIntent.Action) },
        onBack = { /* nav */ },
    )
}

// stateless — previewable, testable, no DI
@Composable
fun XxxScreen(
    state: XxxState,
    onAction: () -> Unit = {},
    onBack: () -> Unit = {},
) { /* UI body */ }

@Preview
@Composable
private fun XxxScreenPreview() {
    UndercurrentTheme {
        XxxScreen(state = XxxState(/* realistic seed */))
    }
}
```

4. **Default lambdas on every callback** (`= {}`) so previews + tests
   don't have to wire every action.
5. **Group sub-Composables by feature, not by widget type.** A
   `components/` subpackage owned by ONE feature — not a shared
   "widgets" graveyard. Cross-feature reusables go in `:core:ui`.
6. **Bundle wide signatures into per-section configs.** When a
   Composable's parameter list crosses ~8 items, group related
   params/callbacks into a config class
   (`ChatHeaderConfig`, `ChatMessagesConfig`, `ChatInputConfig` —
   see `ChatScreen.kt`). Easier to evolve; impossible to mis-order.

## Use case rule

> Use cases are worth their own file ONLY when (a) called from multiple
> places, or (b) they contain real logic — guards, transforms, flow
> composition. Pure 1-line `repo.foo()` delegations go straight in the
> ViewModel.

Current use cases (all in `:core:domain/usecase/chat/`):

- `SelectConversationUseCase` — idempotent guard + load
- `DeleteCurrentConversationUseCase` — null guard + Bool return
- `SelectAgentUseCase` — 3 early-return guards
- `ObserveChatStateUseCase` — combines 4 `ChatRepository` flows into a `ChatStateSnapshot`

That's the entire roster. When something's tempting to extract, check
the two criteria first.

## Per-module Koin DI

No single bloated `AppModule.kt`. Each module publishes its own Koin
module:

- `:core:domain` → `repositoryModule` (KMP repo bindings),
  `repositoryAndroidModule` (Weft impls), `chatUseCasesModule`
- `:core:navigation` → `navigationModule`
- `:data:datastore` → `datastoreAndroidModule` / `datastoreIosModule`
- `:data:sqldelight` → `databaseAndroidModule` / `databaseIosModule`
- `:feature:<name>` → `<name>Module` (binds the ViewModel),
  `<name>AndroidModule` (Weft-backed impls of feature interfaces, where
  applicable)

`:androidApp/.../AppModule.kt` composes them into `allModules`. Same
shape on iOS via `:composeApp/iosMain/.../IosKoinModule.kt`'s
`iosAllModules`.

## App-specific patterns

- **`NavigationChannel`** is how Weft tools navigate. The tool emits to
  the channel; `WeftAppViewModel`/`IosAppViewModel` collects and
  dispatches `NavigationIntent.Navigate(...)`. Keeps the back stack
  honest.
- **Permission denials surface as a dialog.** `WeftAppViewModel` detects
  the permission-denied shape in `ChatChunk.ToolFail`, populates
  `pendingPermissionDialog`, and `MainActivity` renders an AlertDialog
  with an "Open Settings" deep-link. Don't let permission errors land
  as inline toolFail bubbles.
- **MCP integrations need a process restart.** `WeftRuntime` bakes the
  MCP server list at construction. Connect/Disconnect persists the
  change, then prompts the user to restart. `restartProcess(context)`
  in `MainActivity.kt` does the kill-and-relaunch.
- **Anti-hallucination preamble.** `core/AppPreamble.kt` includes the
  "never narrate a tool call you don't make" directive. Don't reformat
  casually — sentences are tuned against agent behavior.
- **Force-stop is important when iterating on tools.** `WeftRuntime`'s
  tool catalog is built once at process start; reinstalling without
  killing the old process means the agent uses the stale catalog.

## Tool-authoring rules

Tool selection is a soft attention process — names + descriptions
determine whether the model picks the tool or silently skips it. Full
guide: `../weft/docs/writing-a-custom-tool.md`. Headlines:

1. **Name: `<verb>_<noun>`, ≤3 words, `lowercase_snake_case`.**
   `open_map`, `set_theme_palette`. Compound prepositions
   (`show_location_on_map`) get skipped.
2. **Description leads with the action.** "Open the map app pinned
   at…" beats "This tool is used when…". 2–4 user-phrasing examples.
3. **Cap descriptions at ~3 sentences.** Longer = tool-skip.
4. **Disambiguate from neighbors.** "NOT for directions — use
   `maps_open_directions` for navigation."
5. **Use `KotlinTypeToken(typeOf<Args>())`, not `typeToken<Args>()`.**
   The app builds JVM 11; substrate's inline `typeToken` is JVM 17
   bytecode and won't inline.

When narrated-but-not-called: force-stop + reinstall → fresh
conversation → rename to verb-noun → shorten description.

## Testing (BDD via kotest)

The convention plugin wires kotest + Turbine + kotlinx-coroutines-test
+ **Mokkery** into every KMP library's `commonTest`; MockK +
kotest-runner-junit5 in `androidUnitTest`. No per-module test deps to
add.

Mokkery is a Kotlin compiler plugin that generates mock implementations
of interfaces at compile time — like MockK, but K/N-native. Use it
directly in `commonTest`; the same module's `androidUnitTest` still has
full JVM MockK for tests that need a feature Mokkery doesn't ship.

**BDD style** — `BehaviorSpec` with `Given` / `When` / `Then`:

```kotlin
class FooViewModelTest : BehaviorSpec({
    val mainDispatcher = StandardTestDispatcher()
    beforeTest { Dispatchers.setMain(mainDispatcher) }
    afterTest { Dispatchers.resetMain() }

    Given("a fresh VM") {
        When("Intent.X is dispatched") {
            Then("state.foo becomes 'bar'") {
                runTest {
                    val repo = mock<FooRepository> {
                        every { fetch(any()) } returns flowOf("bar")
                    }
                    val vm = FooViewModel(repo)
                    vm.dispatch(FooIntent.X)
                    advanceUntilIdle()      // dispatch is async — returns Job
                    vm.state.value.foo shouldBe "bar"
                    verify(exactly(1)) { repo.fetch("x") }
                }
            }
        }
    }
})
```

**Split convention** (when both styles apply):

- `<Name>ViewModelStateTest.kt` in **commonTest** — state-projection
  assertions + collaborator-call verification via Mokkery. Runs on
  Android + iOS.
- `<Name>ViewModelTest.kt` in **androidUnitTest** — only when MockK's
  JVM-only features are needed (advanced spy/argument-capture).
  Defaults to commonTest with Mokkery.

The split is by *what an assertion checks*, not by *what the test
exercises*.

**Always `runTest { advanceUntilIdle() }` before asserting state.**
`MviViewModel.dispatch` returns `Job` — every state change is async on
the test dispatcher.

## Coverage

Kover wired at root, filtered to `*ViewModel` + `*UseCase` classes.

```bash
./gradlew koverHtmlReport     # build/reports/kover/html/index.html
./gradlew koverXmlReport      # for CI tools
```

## Build commands

```bash
# Cross-platform compile (catches features that touch Android-only
# APIs from commonMain).
./gradlew :androidApp:compileDebugKotlin
./gradlew :composeApp:compileKotlinIosSimulatorArm64

# A specific feature.
./gradlew :feature:chat:compileDebugKotlinAndroid
./gradlew :feature:chat:compileKotlinIosSimulatorArm64

# Install + start + tail logs.
adb shell am force-stop dev.weft.undercurrent
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb shell am start -n dev.weft.undercurrent/.MainActivity
adb logcat | grep Undercurrent

# Tests.
./gradlew test                                    # everything
./gradlew :feature:chat:test                      # one module
./gradlew :shared:test                            # MviViewModel base
```

## Conventions

- **No emojis in code or commits** unless the user explicitly asks.
- **Default to zero comments.** Read the code first; comment only when
  the *why* is non-obvious (hidden constraint, subtle invariant,
  workaround). A one-line KDoc on a public API consumed cross-module is
  fine — multi-paragraph treatises on internal symbols are not. Never
  restate what the code does, reference the current PR / task / story,
  or section-header inside a single file. If removing a comment doesn't
  confuse a future reader, the comment shouldn't exist.
- **`internal` by default** for classes that don't cross module
  boundaries. Public is the exception.
- **DataStore for prefs, KeyVault for secrets, SQLDelight (via Weft)
  for conversations + memory.** Don't mix.
- **Async dispatch is the contract.** `dispatch` returns `Job`. Don't
  assume state changes synchronously after a dispatch call.

## What NOT to do

- Don't add app-specific tools to `../weft/tools/`. They belong here
  via `extraToolsFactory` in `AppModule.kt`.
- Don't reach for `viewModelScope.launch { … }` in a ViewModel —
  use the base-class `launch { … }` helper. Same for `collectInto` /
  `observe` instead of hand-rolled `collect + update`.
- Don't wrap individual `when` branches in `launch { }` when the outer
  dispatch already does. Each branch should be a direct `suspend` call.
- Don't create a Use case for a 1-line repo delegation. Inline it in
  the ViewModel.
- Don't depend on Weft from a feature module (breaks iOS). Wrap it
  through a `:core:domain` repository whose Android impl lives in
  `:core:domain/androidMain`.
- Don't inject feature ViewModels into `ScreenRouter`. Each feature's
  `Route` composable does its own injection.
- Don't inject or touch a `*Repository` (or `*Store` / `DataStore`)
  from a Route **or** a Screen. Repositories live in ViewModels; the
  view reads `state` and sends `Intent`s. A `*Repository = koinInject()`
  in a view, or a `repo.someFlow.collectAsState()` in a view, is the
  anti-pattern — fold it into the VM and expose state + intents instead.
- Don't give a Screen a second "stateful overload" that self-injects
  its VM (`fun XxxScreen(…, viewModel: XxxViewModel = koinViewModel())`).
  That re-introduces DI into the view. The Route injects the VM; the
  Screen has exactly one signature — state in, callbacks out.
- Don't call `koinInject` / `koinViewModel` / `collectAsState` inside
  a stateless Screen Composable. That's the `Route`'s job — the Screen
  takes state + callbacks only so `@Preview` and snapshot tests can
  render it. Hoist any session / `StateFlow` a screen reads into a
  plain value parameter.
- Don't ship a Composable file over 300 lines or one without a
  `@Preview`. Split into a `components/` subpackage when it grows.
- Don't put the JVM `mockk` artifact or kotest-runner-junit5 in
  `commonTest` — both are JVM-only. Use Mokkery (`mock<T>()` +
  `every {}` + `verify {}`) which IS multiplatform. Kotest's engine
  + assertions + Turbine ARE KMP and also live in `commonTest`.
- Don't reformat the AppPreamble casually — sentences are tuned against
  agent behavior.
</content>
</invoke>
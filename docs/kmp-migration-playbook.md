# KMP migration playbook

This doc captures the *mechanical* steps to move code out of the old
single-module `:app` into the new KMP-modular structure landed in the
scaffolding commit. Old `app/` is intentionally left on disk as a
reference — pull code out of it incrementally, verify each move
builds, then delete `app/` at the end.

## Status

✅ **Scaffolding shipped** — see `settings.gradle.kts` for the 30
modules now wired in.

❌ **Code not yet migrated** — every new module is empty (only
`.gitkeep` placeholders). This playbook is the recipe for emptying
`app/src/main/kotlin/...` into them.

## The three decisions in effect

These are the architectural rules every migration step honors:

1. **iOS UI: Compose Multiplatform.** Shared composables live in
   `commonMain` of `:composeApp` and `:feature:*`. iOS-only Swift
   code is restricted to the `iosApp/` Xcode project shell.
2. **Weft stays Android-only.** Feature modules must NOT depend on
   Weft directly — they'd break iOS compilation. All Weft access
   goes through `:data:weft` (Android-only). Common UI talks to a
   `WeftEngine` interface (declared in `:shared`) that has an
   Android impl in `:data:weft` and a stub iOS impl that returns
   "not supported".
3. **Convention plugins for build wiring.** Module `build.gradle.kts`
   files are tiny — they apply a convention plugin
   (`undercurrent.kmp.feature` etc.) and add module-specific
   dependencies. Anything common belongs in `build-logic/`.

## Where each kind of code goes

| Today in `app/src/main/kotlin/dev/weft/undercurrent/...` | Goes to |
|---|---|
| `core/AppState.kt`, `AppIntent.kt`, `Screen.kt` | `:shared` (commonMain) — pure data + sealed interfaces |
| `core/AppStore.kt` (ViewModel + agent wiring) | Split: state machine in `:shared`; Weft binding in `:data:weft`; Android ViewModel shell in `:androidApp` |
| `core/MainActivity.kt`, `Application.kt`, OAuth deep-link plumbing | `:androidApp` (`src/main/kotlin/...`) |
| `di/AppModule.kt` (Koin) | Split per concern: each feature's Koin module lives in `:feature:<name>`; cross-feature wiring in `:composeApp` |
| `theme/*` (colors, typography, spacing) | `:core:design-system` (commonMain) |
| `components/*` (shared composables like LoadingState) | `:core:ui` (commonMain) |
| `features/chat/*` | `:feature:chat` |
| `features/personas/*` | `:feature:personas` |
| `features/memories/*` | `:feature:memories` |
| ...every other `features/<name>/` | `:feature:<name>` |
| `data/MyRepository.kt` | `:data:repository` (commonMain or androidMain depending on what it touches) |
| SQLDelight `.sq` files in `sqldelight/` | `:data:sqldelight/src/commonMain/sqldelight/...` |
| Android resources (drawables, strings, themes) | `:androidApp/src/main/res` for app-level; `:core:resources/src/androidMain/res` for shared |
| Composable images / icons used cross-feature | `:core:resources` (use compose-resources for CMP) |

## Per-feature checklist

For each feature in `app/.../features/<name>/`:

### 1. Identify what the feature owns

```bash
ls app/src/main/kotlin/dev/weft/undercurrent/features/<name>/
```

Typical contents:
- `<Feature>Screen.kt` — Compose UI
- `<Feature>ViewModel.kt` — state + dispatch (or store-pattern reducer)
- `<Feature>Repository.kt` — persistence (sometimes)
- `<Feature>Tool.kt` — Weft tools this feature contributes
- `<Feature>Module.kt` — Koin module

### 2. Split by destination

| File | New home |
|---|---|
| `<Feature>Screen.kt` | `:feature:<name>/src/commonMain/kotlin/...` |
| `<Feature>ViewModel.kt` (pure state) | `:feature:<name>/src/commonMain/kotlin/...` |
| `<Feature>ViewModel.kt` (touches Weft) | Refactor: pure VM in `commonMain` talks to an interface; Android impl in `androidMain` or `:data:weft` |
| `<Feature>Repository.kt` (pure Kotlin) | `:data:repository/src/commonMain/kotlin/...` OR feature-local in `:feature:<name>/commonMain` |
| `<Feature>Repository.kt` (Android APIs) | `:data:repository/src/androidMain/kotlin/...` |
| `<Feature>Tool.kt` (Weft tool subclass) | `:data:weft/src/main/kotlin/...` — Weft is Android-only |
| `<Feature>Module.kt` (Koin) | `:feature:<name>/src/commonMain/kotlin/.../di/<Feature>Module.kt` |

### 3. Refactor for KMP-friendliness

If the feature uses Android-only APIs in a place that needs to compile
on iOS too, introduce an `expect`/`actual` split or extract an
interface:

```kotlin
// :shared commonMain
interface MyPlatformThing { suspend fun doStuff(): String }

// :shared androidMain
class AndroidMyPlatformThing(...) : MyPlatformThing { ... }

// :shared iosMain (if iOS needs to work)
class IosMyPlatformThing(...) : MyPlatformThing { ... }
```

If the iOS variant isn't ready, throw `NotImplementedError("iOS variant TBD")`.

### 4. Add the dependencies to the feature's build.gradle.kts

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.data.repository)
            // ...feature-specific deps
        }
        androidMain.dependencies {
            implementation(projects.data.weft)  // ONLY if feature touches Weft tools
        }
    }
}
```

### 5. Verify the feature module compiles

```bash
./gradlew :feature:<name>:compileKotlinAndroid     # JVM/Android compile
./gradlew :feature:<name>:compileKotlinIosArm64    # iOS compile
```

If the iOS compile fails because something references Android APIs,
fix the source-set assignment (move to `androidMain`) or introduce
an interface boundary as in step 3.

### 6. Delete the old `app/.../features/<name>/` directory

Confirm nothing else imports the old paths. Run a full build:

```bash
./gradlew :androidApp:assembleDebug
```

### 7. Commit

One commit per feature: "Migrate :feature:<name> to KMP".

## Specific feature notes

These features have quirks worth flagging up front.

### `:feature:chat` (the big one)

- The streaming agent + tool-call rendering UI is the most
  complex. Plan to spend a full day on this one.
- The streaming chunks (`StreamChunk.TextDelta`, etc.) come from
  Weft (`:data:weft`). Define an `AgentEngine` interface in
  `:shared/commonMain` that emits a KMP-friendly `Flow<ChatChunk>`;
  the Android impl wraps `WeftAgent.sendStreaming()`.

### `:feature:personas`, `:feature:providers`, `:feature:keypaste`

- These touch the Weft `KeyVault` (Android Keystore). The Android
  impl in `:data:weft` exposes a `KeyVaultGateway` that the feature
  consumes through `:shared`. iOS impl uses Keychain — write a stub
  for v1 that throws `NotImplementedError`.

### `:feature:integrations`

- OAuth flows go through Weft's `:oauth` (Android-only). Same
  pattern as KeyVault — wrap behind an interface, Android impl
  delegates, iOS stub for v1.

### `:feature:voice`

- TTS / STT come from Weft `Speech` capability. Wrap in `:data:weft`.

### `:feature:memories`, `:feature:conversations`, `:feature:traces`

- These read from Weft's SQLDelight stores (memory, conversation,
  trace). The reads happen via Weft APIs in Android, so put the
  ViewModel + repository in `:data:weft` (Android-only) or expose a
  query interface from `:shared` that Android backs with Weft and
  iOS stubs.

### `:feature:miniapps`

- Compose components the LLM renders via `ui_render`. Pure Compose;
  move straight to `:feature:miniapps/src/commonMain/`. No Weft
  dependency at the UI layer.

### `:feature:onboarding`

- Mostly Compose + DataStore (preferences). Move to commonMain;
  use `multiplatform-settings` lib (not yet wired in libs.versions —
  add when needed) instead of `androidx.datastore`.

## The Weft bridge — what `:data:weft` should expose

The `:data:weft` Android-only module is the gatekeeper. Feature
modules in commonMain depend on interfaces in `:shared`; `:data:weft`
provides Android implementations that delegate to Weft.

Minimum shape:

```kotlin
// :shared/commonMain/kotlin/dev/weft/undercurrent/agent/AgentEngine.kt
interface AgentEngine {
    val state: StateFlow<AgentState>
    suspend fun send(text: String): String
    fun sendStreaming(text: String): Flow<ChatChunk>
    suspend fun newChat()
    suspend fun resume(conversationId: String)
    // ... whatever the UI needs
}

// :data:weft/src/main/kotlin/.../AndroidAgentEngine.kt
class AndroidAgentEngine(
    private val runtime: WeftRuntime,
    // ...
) : AgentEngine {
    // delegate to WeftRuntime / WeftAgent
}
```

The Koin module in `:composeApp/commonMain` looks like:

```kotlin
val agentModule = module {
    // Provided per-platform:
    //   androidMain: AndroidAgentEngine wrapping WeftRuntime
    //   iosMain: stub that throws NotImplementedError for v1
    single<AgentEngine> { getAgentEngine() }  // expect/actual
}

expect fun Scope.getAgentEngine(): AgentEngine
```

## Phase-by-phase migration order

Recommended order (each phase is one PR):

1. **Phase 1 — core modules.** Move pure-Kotlin types into
   `:core:model`, extensions into `:core:ext`, design tokens
   (colors, typography) into `:core:design-system`. No business
   logic moves yet; this just establishes the shape.

2. **Phase 2 — shared business interfaces.** Define `AgentEngine`,
   `KeyVaultGateway`, `OAuthGateway` interfaces in `:shared/commonMain`.
   Implement them in `:data:weft` against Weft. Wire up Koin modules.

3. **Phase 3 — leaf features.** Migrate the simplest features
   first: `:feature:onboarding`, `:feature:settings`,
   `:feature:miniapps` (pure Compose, no Weft).

4. **Phase 4 — Weft-touching features.** `:feature:personas`,
   `:feature:providers`, `:feature:keypaste`, `:feature:memories`,
   `:feature:conversations`, `:feature:traces`,
   `:feature:integrations`, `:feature:voice`.

5. **Phase 5 — `:feature:chat`.** The big one. Last because
   everything else exercises the cross-module patterns first.

6. **Phase 6 — `:androidApp` shell + `:composeApp` wiring.** Move
   MainActivity, Application, OAuth deep-link handling, theme
   integration. Verify `./gradlew :androidApp:assembleDebug` produces
   a working APK.

7. **Phase 7 — `iosApp/` Xcode project.** Stand up the SwiftUI shell
   that embeds the `ComposeApp.framework`. Initially a single-screen
   app that says "hello"; expand once the framework export works.

8. **Phase 8 — delete `app/`.** Once `:androidApp` is feature-
   complete, drop the old single-module directory.

## Verification commands

```bash
# Verify Gradle topology is healthy.
./gradlew projects

# Verify a single module compiles for both targets.
./gradlew :feature:<name>:compileKotlinAndroid
./gradlew :feature:<name>:compileKotlinIosSimulatorArm64

# Verify the full Android app builds.
./gradlew :androidApp:assembleDebug

# Verify the iOS framework links (once :composeApp has real code).
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# Run all tests.
./gradlew allTests
```

## Known issues + workarounds

### Weft pulls okhttp-sse 4.x → 5.x mismatch

The Weft SDK's libs.versions.toml forces `okhttp-sse = 5.3.2`.
Undercurrent's libs.versions.toml mirrors this. If you upgrade
Ktor or Koog later, re-verify the OkHttp + okhttp-sse pair stay in
sync.

### kotlinx-datetime pinned to 0.7.1

Weft pinned `kotlinx-datetime` to 0.7.1 (the Koog → AWS-SDK chain
resolves to it transitively). Undercurrent inherits this. Using
`kotlinx.datetime.Clock` or `kotlinx.datetime.Instant` won't work
at runtime — use `kotlin.time.Clock` / `kotlin.time.Instant`
(stdlib in Kotlin 2.x). Annotate with
`@OptIn(kotlin.time.ExperimentalTime::class)` if needed.

### iOS SQLDelight needs a custom Compose-friendly driver

The native SQLDelight driver works fine, but the schema migration
verifier is JVM-only. Migration tests should run on Android via
JdbcSqliteDriver (already wired in Weft's MigrationTest).

### lintVital* tasks disabled

The root `build.gradle.kts` disables `lintVital*` tasks because
AGP 8.7.3 lint has a JDK 25 incompatibility. Re-enable when AGP
catches up.

## When in doubt — what to migrate to which source set

```
                 +--------------------------------+
                 |        commonMain              |
                 |  - data classes                |
                 |  - sealed interfaces           |
                 |  - pure-Kotlin Repos           |
                 |  - Compose UI                  |
                 |  - Koin modules                |
                 +---------------+----------------+
                                 |
                  +--------------+---------------+
                  |                              |
        +---------v---------+         +---------v---------+
        |    androidMain    |         |     iosMain       |
        |  - Weft bindings  |         |  - stubs / native |
        |  - Android-only   |         |  - Keychain       |
        |    services      |         |  - Darwin Ktor    |
        |  - Glide / Coil   |         |  - iOS-only       |
        |  - ML Kit         |         |    integrations   |
        +-------------------+         +-------------------+
```

When unsure: start in `commonMain`. The compiler will tell you if
something needs to move to `androidMain` or `iosMain`. Don't
preemptively split.

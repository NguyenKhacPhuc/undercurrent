# KMP migration — current status

Last updated: post-host-shell + iOS-Kotlin-ready commit (`7a5a977`).
Pair with [`kmp-migration-playbook.md`](kmp-migration-playbook.md) for
the per-feature recipe; this doc is for "where are we now and what
still needs doing."

---

## TL;DR — what works today

**Android: fully functional. iOS: builds, but core chat is hollow.**

| | Android | iOS |
|---|---|---|
| **Build target** | `./gradlew :androidApp:assembleDebug` → APK | `./gradlew :composeApp:linkDebugFrameworkIos{Arm64,SimulatorArm64}` → `ComposeApp.framework` |
| Launch & render UI | ✅ | ⚠️ needs `iosApp/` Xcode project (see [iOS roadmap](#ios-roadmap)) |
| Theme / Appearance / Settings | ✅ | ✅ (once Xcode shell exists) |
| Personas / Memories / Traces / Usage / Integrations / Conversations | ✅ | ✅ (renders against stub gateways — empty data) |
| Onboarding → KeyPaste → Chat handshake | ✅ | ⛔ stalls at KeyPaste — `StubKeyVaultGateway` no-ops |
| **Send a chat message** | ✅ (Koog → Anthropic/OpenAI/OpenRouter/DeepSeek) | ⛔ `IosAppStore` is a stub |
| Tool calls (calendar/location/notify/files/share/etc.) | ✅ | ⛔ no agent |
| `ui_render` mini-apps + cached-tree replay | ✅ | ⛔ no agent |
| OAuth integrations (Linear, etc.) | ✅ | ⛔ `StubOAuthGateway` returns `UserCancelled` |
| Voice input | ✅ (Android `SpeechRecognizer`) | ⛔ `StubSpeechGateway` no-ops |
| Conversation / memory / trace persistence | ✅ (SQLDelight + DataStore) | ⚠️ DataStore + SQLite work, but no agent writes to them |
| Key storage | ✅ (Android Keystore-encrypted prefs) | ⛔ stub |

**Why iOS is hollow today:** Weft's agent loop depends on Koog 1.0.0,
which publishes only `-jvm` and `-android` variants. See [The Koog
blocker](#the-koog-blocker).

**The plan:** don't try to port Weft. Ship a minimal Ktor-based
Anthropic client for iOS (~600 LOC total: Keychain + HTTP client +
real `IosAppStore`), stub everything else with clear "not on iOS yet"
messages. Each phase in [iOS roadmap](#ios-roadmap--minimal-first-incremental)
ships incremental value; the first usable iOS build is ~1 week of
work, not months.

---

## Architecture — where everything lives

### Module map

```
weft/   (sibling SDK repo; included via composite-build)
  ├─ :contracts         KMP   ← interfaces, types
  ├─ :security          KMP   ← NetworkPolicy + Redactor
  ├─ :harness:skills    KMP   ← Skill / SkillRegistry / withHelp
  ├─ :harness:agents    JVM   ← WeftAgent, streaming, model routing (Koog)
  ├─ :harness:prompt    JVM   ← prompt assembly + cache binder (Koog)
  ├─ :harness:memory    JVM   ← MemoryStore
  ├─ :harness:conversation  JVM  ← ConversationStore
  ├─ :harness:observability JVM  ← TraceStore + Redactor wiring
  ├─ :harness:cost      JVM   ← UsageStore
  ├─ :harness:behavior  JVM
  ├─ :harness:reliability   JVM  ← CircuitBreaker
  ├─ :tools             JVM   ← WeftTool + 30+ built-in tools
  ├─ :mcp               JVM   ← MCP client
  ├─ :oauth             Android  ← Custom Tabs + token store
  ├─ :os-bridge         Android  ← Keystore, Intents, biometrics
  ├─ :android           Android  ← WeftRuntime composition root
  ├─ :android-compose   Android  ← ComposeUiBridge + TreeRenderer
  └─ :android-compose-defaults  Android  ← default component palette

undercurrent/
  ├─ :core:model        KMP   ← AppEffect, ProviderKind, ModelTier, Persona, MiniApp, ThemePrefs, AppPalette
  ├─ :core:design-system    KMP  ← UndercurrentColors/Typography/Shapes/Theme, palette colors
  ├─ :core:ui           KMP   ← LoadingPlaceholder, ScreenScaffold, TipBox, TokenDivider, SectionLabel
  ├─ :core:navigation   KMP   ← Screen + NavigationChannel
  ├─ :core:resources    KMP
  ├─ :shared            KMP   ← AgentEngine + 10 gateway interfaces + mirror types + iOS stubs
  ├─ :data:datastore    KMP   ← 7 repos + createPreferencesDataStore (Android + iOS actuals)
  ├─ :data:sqldelight   KMP   ← UndercurrentDatabase + DatabaseDriverFactory actuals
  ├─ :data:weft         Android  ← 10 gateway impls (Weft-backed) + Weft tools
  ├─ :feature:* (17)    KMP   ← per-screen ViewModels + Composables
  ├─ :composeApp        KMP   ← App() + AppState + AppIntent + AppStore + ScreenRouter + PlatformAdapter
  │     ├─ commonMain   shared root composable + 11 KMP-clean routes
  │     ├─ androidMain  (currently empty)
  │     └─ iosMain      IosAppStore + iosPlatformAdapter + iosAppModule + initKoin + MainViewController
  └─ :androidApp        Android  ← MainActivity + UndercurrentApp + WeftAppStore + AppModule (Koin) + ChatRoute + components/
```

### How a screen reaches its data

For a KMP-clean screen (e.g. Personas):

```
ScreenRouter (commonMain) → PersonasScreen (commonMain)
   → PersonasViewModel via koinViewModel (commonMain)
   → PersonaRepository (commonMain, :data:datastore)
   → DataStore<Preferences> (KMP, Android + iOS actuals)
```

For a substrate-coupled screen (e.g. Chat):

```
ScreenRouter (commonMain)
   → platform.chatRoute()  ← PlatformAdapter @Composable lambda
       Android: ChatRoute  → ChatScreen + drawer + ChatStore + Weft
       iOS:     "Chat — coming to iOS" placeholder
```

For mirror types crossing the Android boundary:

```
AppState (commonMain)
   ↔ WeftAppStore (Android)
       ProviderKind.toWeft() / toMirror() converters
       ModelTier.toWeft() / toMirror() converters
       UiRenderEvent mirror ↔ UIUpdate.RenderTree
```

### Foundation modules — DONE

| Module | Contents |
|---|---|
| `:core:model` | AppEffect, ThemeMode/ThemePrefs/AppPalette, ProviderKind, ModelTier, Persona/PersonaKind/BuiltInPersonas, MiniApp |
| `:core:design-system` | UndercurrentColors/Shapes/Typography/Theme, 4 palette color tables + `AppPalette.colors(dark)` |
| `:core:ui` | LoadingPlaceholder, ScreenScaffold, ScaffoldTextAction, TokenDivider, SectionLabel, TipBox |
| `:core:navigation` | `Screen` sealed interface + `NavigationChannel` |
| `:shared` | `AgentEngine` interface + 10 gateways (`KeyVault` / `OAuth` / `Conversation` / `Memory` / `Trace` / `Usage` / `ModelCatalog` / `Speech` / `UiBridge` / `KeyValidation`) + mirror types + iOS stubs |
| `:data:weft` (Android-only) | All 10 gateway impls + Weft tools (`NavigationTools`, `CreatorTools`, `ShowLocationOnMapTool`) |
| `:data:datastore` | 7 KMP-published repos + `createPreferencesDataStore(name)` (Android+iOS actuals) |
| `:data:sqldelight` | `UndercurrentDatabase` (Records.sq) + `DatabaseDriverFactory` (Android+iOS actuals) |

### Feature modules — 17 / 17 DONE

All commonMain. Per-screen breakdown is in the migration history (see
git log `:feature:*` commits) and the recipes section at the bottom.

### Orchestrator layer (commonMain App composable)

```
:composeApp/commonMain/dev/weft/undercurrent/app/
├─ AppState.kt          ← agent: WeftAgent? replaced by agentReady + currentConversationId
├─ AppIntent.kt         ← UIUpdate? → UiRenderEvent mirror; InvokeMiniApp carries cachedRenderTreeJson
├─ AppStore.kt          ← interface exposing only mirror DTOs (SkillSummary, DisplayMessage, AppState)
├─ App.kt               ← theme + snackbar + permission-needed dialog + router
├─ ScreenRouter.kt      ← 11 KMP-clean routes inline; 4 substrate-coupled routes delegate to PlatformAdapter
└─ PlatformAdapter.kt   ← @Composable lambdas for Chat/RenderedTree/Creator/MiniApps + OS bridges
```

Android impl (`:androidApp/.../core/`):

```
WeftAppStore.kt    ← implements AppStore; uses WeftRuntime + WeftAgent; subscribes to UiBridgeGateway.renderEvents
MainActivity.kt    ← FragmentActivity + OAuth deep links + constructs PlatformAdapter (Android-side composables)
UndercurrentApp.kt ← Application class; starts Koin with appModule
AppPreamble.kt     ← ASSISTANT_APP_PREAMBLE constant
```

iOS impl (`:composeApp/iosMain/.../app/`):

```
IosAppStore.kt           ← stub AppStore — Resume → Onboarding so iOS lands on real UI
iosPlatformAdapter()     ← placeholder composables for substrate routes; no-op OS bridges
IosKoinModule.kt         ← iosAppModule binds DataStores + repos + 11 stub gateways + AppStore + VMs
InitKoin.kt              ← startKoin() exported for Swift
MainViewController.kt    ← ComposeUIViewController { App(...) } — Swift entry point
```

---

## Build verification

```bash
# Weft side (run from /Users/phucnguyen/Documents/mas/weft)
./gradlew :contracts:compileKotlinIosSimulatorArm64
./gradlew :security:compileKotlinIosSimulatorArm64 :security:jvmTest
./gradlew :harness:skills:compileKotlinIosSimulatorArm64
./gradlew :android:assembleDebug   # downstream consumers still happy

# Undercurrent side (run from /Users/phucnguyen/Documents/mas/undercurrent)
./gradlew :androidApp:assembleDebug                         # APK
./gradlew :composeApp:linkDebugFrameworkIosArm64            # device framework
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64   # simulator framework
```

All five tasks → `BUILD SUCCESSFUL` as of commit `7a5a977`
(undercurrent) + `ccd2bc1` (weft).

---

## The Koog blocker

Weft's agent loop is built on JetBrains' Koog 1.0.0. Koog's Maven
artifacts ship only `-jvm` and `-android` variants:

```
~/.gradle/caches/modules-2/files-2.1/ai.koog/
├── agents-core-android/
├── agents-core-jvm/                    ← no -ios, no -native
├── prompt-executor-anthropic-client-android/
├── prompt-executor-anthropic-client-jvm/
└── … (every Koog module follows this pattern)
```

Consequence: every Weft module that depends on Koog cannot KMP-ify
without either upstream Koog iOS targets or a custom replacement.
That's all of:

- `:harness:agents`, `:harness:prompt`, `:harness:memory`,
  `:harness:conversation`, `:harness:observability`, `:harness:cost`,
  `:harness:behavior`, `:harness:reliability`
- `:tools` (uses Koog `agents-tools`)
- `:mcp` (uses Koog HTTP client)

### What we KMP-ified anyway

Three Weft modules that don't touch Koog were converted in commit
`ccd2bc1`:

- `:contracts` — KMP, all targets
- `:security` — KMP (Ktor is already KMP; `NetworkPolicy` works on iOS)
- `:harness:skills` — KMP

This shrinks the mirror layer in `:shared` slightly (skills can be
real Weft types now).

### Why we're not waiting on Koog

The natural reaction to "Koog has no iOS variants" is "let's wait for
JetBrains to publish them, or KMP-port the agent loop ourselves." Both
are months of work for an open-ended outcome.

The iOS roadmap below picks a different shape: **don't reuse Weft on
iOS at all.** Weft's agent loop is sophisticated — tool catalog,
model routing, MCP, ui_render, traces, cost tracking, circuit
breakers. iOS v1 doesn't need any of that. It needs *one text
conversation against Anthropic*, period. ~600 LOC of Ktor + Keychain.
Everything else stubbed.

This is option **C** from the earlier session, scoped minimally and
written in Kotlin (`:composeApp/iosMain`), not as a separate Swift
codebase.

---

## iOS roadmap — minimal-first, incremental

**Strategy:** ship a minimal vertical slice (text-only chat against
Anthropic) and stub everything else as "not on iOS yet." Don't try to
port all of Weft. Each phase below ships an actually-usable
incremental improvement; stop at whichever phase is good enough.

The Koog blocker means we can't reuse the agent loop — but we don't
need to. A minimal chat is **~300 lines of Ktor calling the Anthropic
Messages API.** Tools, OAuth, voice, mini-apps, traces — all
deferred or stubbed.

### Phase 0 — Xcode shell (~1 hour, needs Mac)

Create `iosApp/iosApp.xcodeproj` with SwiftUI `@main`:

```swift
import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() { Main_iosKt.doInitKoin() }
    var body: some Scene {
        WindowGroup { ComposeView().ignoresSafeArea() }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        Main_iosKt.MainViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
```

Embed framework via a Run Script build phase:

```
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
```

Framework Search Paths:
`$(SRCROOT)/../composeApp/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`.

After this: `Cmd-R` launches an iOS Simulator app that boots,
mounts `App()`, lands at `Screen.Onboarding`. Settings + theme
screens render. Chat tab shows "Chat — coming to iOS" placeholder.

### Phase 1 — Minimal viable chat (~3-5 days)

The smallest amount of code that gets the user from KeyPaste → "I can
have a text conversation with the assistant." No tools, no streaming,
no OAuth, no voice. Just: type → wait → see the reply.

Components needed:

1. **iOS Keychain `KeyVaultGateway`** (~150 LOC)
   - Replace `StubKeyVaultGateway` with an impl using
     `platform.Security.SecItemAdd` / `SecItemCopyMatching`.
   - One generic-password item per provider alias.
   - Unblocks: KeyPaste → key persists → boot advances past KeyPaste.

2. **Minimal Anthropic Ktor client** (~200 LOC)
   - Lives in `:composeApp/iosMain` (or a new `:data:ios-agent`
     module if it grows). Ktor `HttpClient(Darwin)` posting to
     `https://api.anthropic.com/v1/messages`.
   - One-shot request/response (no SSE streaming yet).
   - Just `text → text`. Pass a `messages: List<{role, content}>`
     history.
   - **Skip** tool_use, system prompts beyond a minimal one,
     model routing — pin to `claude-haiku-4-5` or similar.

3. **Real `IosAppStore`** (~250 LOC)
   - Replaces today's `IosAppStore` stub.
   - `dispatch(SendChat)` → append user message to
     `displayMessages` → call the Anthropic client → append assistant
     reply.
   - `dispatch(SubmitKey)` → persist via Keychain gateway → set
     `agentReady = true` → navigate to Chat.
   - Holds the message history in a `MutableStateList` (no persistence
     yet — Phase 2 adds SQLDelight).

4. **Real iOS `ChatRoute`** in `iosPlatformAdapter`
   - Replace the "Chat — coming to iOS" placeholder with a real
     call to the commonMain `ChatScreen`. Most of `ChatScreen`'s
     params are already KMP — wire `displayMessages` from
     `IosAppStore`, no-op the drawer / mini-apps / agent-selector
     bits.

5. **Stub everything else with clear "not yet" messages**
   - `StubOAuthGateway` already returns `UserCancelled` ✓
   - `StubSpeechGateway` no-ops ✓
   - Hide / disable the Tools / Integrations / MiniApps / Creator
     sections in the drawer when on iOS (a `LocalPlatform.current`
     `expect/actual` or just a flag on `PlatformAdapter`).

**Phase 1 deliverable:** iOS user pastes API key → can have a
conversation with Claude. Settings/theme/personas work. Anything
agent-tool-shaped politely says "not available on iOS yet."

### Phase 2 — Polish (~1 week, post-launch)

- **Streaming** — Anthropic SSE → emit `StreamChunk.TextDelta` to
  match Android UX.
- **Conversation persistence** — add `Conversations.sq` schema to
  `:data:sqldelight`, write a real iOS `ConversationStoreGateway`
  impl using SQLDelight. Reuses Android schema → easy port.
- **OS bridges** — wire `onOpenUrl` / `onCopyText` /
  `onOpenAppDetailsSettings` to UIKit (4 small Kotlin/Native interop
  functions).
- **Multi-provider** — extend the Ktor client to OpenAI + OpenRouter
  + DeepSeek. Each is ~50 LOC of endpoint + auth-header swapping.

### Phase 3 — One or two iOS tools (~1-2 weeks, if needed)

Pick the highest-value tools and write iOS impls. Suggested order:

1. **`notify_show`** — `UNUserNotificationCenter`. ~50 LOC. Useful
   for reminder-shaped use cases.
2. **`share_text`** — `UIActivityViewController`. ~30 LOC.
3. **`web_fetch`** — already pure Kotlin if we use Ktor; could go
   commonMain.

Everything else (calendar, contacts, location, files, bluetooth,
camera, biometrics, …) stays "not available on iOS yet" until
demand justifies the per-tool iOS interop work.

### Phase 4 — Long tail (open-ended)

- More tools
- OAuth integrations (Linear, etc.) via `ASWebAuthenticationSession`
- Voice input via `SFSpeechRecognizer`
- `ui_render` mini-apps (need the substrate's component renderer or
  a CMP equivalent)
- Memory + trace persistence

### What deliberately stays "not yet" — even at v1

| Capability | iOS plan |
|---|---|
| Calendar / Contacts / Location / Files / Bluetooth / Camera tools | "Not available on iOS yet" — Phase 4 |
| Biometrics | "Not available on iOS yet" — needs `LAContext` wrapper |
| `ui_render` mini-apps | "Not available on iOS yet" — needs `ComposeUiBridge` rewrite |
| OAuth integrations | "Not available on iOS yet" — Phase 4 |
| Multi-agent declarations | "Not available on iOS yet" — Phase 4 |
| MCP servers | "Not available on iOS yet" — needs KMP-port of `:mcp` |
| Voice input | "Not available on iOS yet" — Phase 4 |
| Cost / usage tracking | Skip on iOS v1; reuse Android-only |
| Trace store | Skip on iOS v1 |
| Memory store | Skip on iOS v1 (`memory_store` / `memory_recall` tools say "not yet") |
| Skills (`/help`, custom commands) | Works — already KMP. Ship as-is. |

### Why this is the right shape

- **Time to first useful iOS build is days, not months.** Phase 0+1 ≈ 1 week.
- **Each phase ships actual value.** Stop at any phase and the iOS
  app does *something* real.
- **No fight with Koog.** We don't pretend Weft can KMP-ify; we just
  bypass the agent loop on iOS for v1.
- **No two-codebase trap.** The "iOS agent" is ~600 LOC of Kotlin
  (Ktor + Keychain + a minimal store). Maintained in `:composeApp/iosMain`
  or a sibling module. Not a separate Swift codebase.
- **Honest messaging.** Anything the iOS user can't do shows a clear
  "not yet" rather than silent failure.

---

## Known limits

- **Material3 `DatePicker` crashes on iOS.** kotlinx-datetime is
  pinned to 0.7.1 (transitive constraint from AWS-SDK, see CLAUDE.md
  in weft repo); Material3's iOS `KotlinxDatetimeCalendarModel` calls
  0.6.x-style APIs that were renamed in 0.7. Linker emits info-level
  warnings; framework still links but `DatePicker` will fail at
  runtime if invoked. Avoid in commonMain screens or guard with
  `expect/actual`.
- **`AppStore` is bound as `single<AppStore>`, not `viewModel<...>`.**
  The commonMain interface can't extend `androidx.lifecycle.ViewModel`
  (iOS wouldn't compile cleanly). `WeftAppStore` still extends
  ViewModel internally for `viewModelScope`; Koin's singleton survives
  configuration changes.
- **`koinViewModel` is `org.koin.compose.viewmodel.koinViewModel`.**
  The Android-only `org.koin.androidx.compose.koinViewModel` will
  shadow this in mixed-import IDE completions — pin the right one.
- **`Material icons-extended` isn't in CMP commonMain.** Replace
  unicode glyphs: `▾` (ArrowDropDown), `›` (KeyboardArrowRight), `←`
  (back), `×` (Close); use plain "Show"/"Hide" text for
  password toggles.
- **JVM target inconsistency.** All convention plugins force
  JVM_17 — both `compileOptions { sourceCompatibility = VERSION_17 }`
  AND `kotlin.compilerOptions { jvmTarget = JVM_17 }`. Forgetting the
  second produces "Inconsistent JVM Target Compatibility" errors.

---

## Reference: patterns that worked

### Recipe A — feature with Weft-store reads only
Examples: `:feature:conversations`, `:feature:memories`,
`:feature:traces`, `:feature:usage`.

1. Define `<Feature>Gateway` interface in `:shared/commonMain/.../gateway/`.
2. Android impl in `:data:weft` wrapping the corresponding Weft store.
3. iOS stub in `:shared/iosMain/.../gateway/`.
4. Move ViewModel to `:feature:<name>/commonMain` — takes the gateway.
5. Move Screen to `:feature:<name>/commonMain` — public, gateway-typed.

### Recipe B — feature with DataStore prefs only
Examples: `:feature:theme`, `:feature:onboarding`, parts of
`:feature:miniapps` + `:feature:personas`.

1. Repository already migrated to `:data:datastore/commonMain`.
2. Constructor takes the migrated repo directly. No gateway needed.
3. Move Screen + ViewModel to `:feature:<name>/commonMain`.

### Recipe C — feature with OAuth / KeyVault / agent tools
Examples: `:feature:integrations`, `:feature:keypaste`,
`:feature:creator`, `:feature:voice`.

1. Define gateway interface in `:shared`.
2. Android impl in `:data:weft` delegates to the Weft contract.
3. iOS stub returns "not supported" / empty.
4. Migrate ViewModel + Screen consuming the interface.

### Recipe D — `:feature:chat`
The hardest one. Consumes `SpeechGateway` + a chat-surface contract.
The `ChatScreen` itself is commonMain but the **drawer-wrapped chat
route** (with conversation flow + add-to-chat sheet + AgentSelector
wiring) lives in `:androidApp/.../core/MainActivity.kt:ChatRoute`
behind the `PlatformAdapter.chatRoute` slot.

### Lift-to-lambda pattern
When a feature screen needs an Android-only Composable or static
call, **take it as a constructor lambda**. The host wires it; the
screen stays commonMain. Examples:
- `MiniAppsScreen(treePreview: @Composable (treeJson, onTap) -> Unit)`
- `OnboardingScreen(modelCountFor: (ProviderKind) -> Int)`
- `KeyPasteScreen(onOpenConsole: (String) -> Unit)`

### Mirror-type pattern
For Weft types feature modules need but can't import (Weft is
Android-only), define a mirror in `:shared/commonMain/.../gateway/`
with the same wire shape. The Android gateway impl translates
between mirror and Weft type at the boundary. Examples:
- `ConversationSummary`, `MemoryEntry`, `AgentTrace`,
  `LlmCallTrace`, `ToolCallTrace`, `UsageTotals`, `ModelInfo`,
  `VoiceState`, `ComponentNode`, `UiRenderEvent`, `OAuthConfig`,
  `OAuthTokens`, `OAuthResult`.

### Type-rewrite gotchas
- `java.util.Date` / `SimpleDateFormat` / `java.text.*` /
  `java.time.*` → `kotlinx.datetime` + `kotlin.time.Clock`.
- `String.format("%.Nf", v)` doesn't exist in commonMain. Write a
  small `formatDecimal(v, n)` helper.
- `Map.toSortedMap()` doesn't exist in commonMain. Use
  `entries.sortedBy { it.key }`.
- `System.currentTimeMillis()` → `kotlin.time.Clock.System.now().toEpochMilliseconds()`.

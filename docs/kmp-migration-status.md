# KMP migration — current status

Last updated: post-host-shell + iOS-Kotlin-ready commit (`7a5a977`).
Pair with [`kmp-migration-playbook.md`](kmp-migration-playbook.md) for
the per-feature recipe; this doc is for "where are we now and what
still needs doing."

---

## TL;DR — what works today

**Android: fully functional via Weft agent. iOS: text chat works
end-to-end via a parallel Ktor stack — tools / OAuth / voice / mini-
apps still stubbed.**

| | Android | iOS |
|---|---|---|
| **Build target** | `./gradlew :androidApp:assembleDebug` → APK | Xcode → ⌘R from `iosApp/iosApp.xcodeproj` |
| Launch & render UI | ✅ | ✅ |
| Theme / Appearance / Settings | ✅ | ✅ (persists via DataStore-Preferences) |
| Personas (voice + role) | ✅ (Weft `extraVolatilePrefix`) | ✅ (composed into per-turn system prompt by `IosAppStore`) |
| Onboarding → KeyPaste → Chat handshake | ✅ | ✅ (persists across restart) |
| **Send a chat message** | ✅ via Koog | ✅ via Ktor — streaming SSE |
| Multi-provider (Anthropic / OpenAI / OpenRouter / DeepSeek) | ✅ | ✅ (one `LlmClient` impl per provider) |
| Multi-conversation history | ✅ (Weft `ConversationStore`) | ✅ (SQLDelight `Conversations.sq`; Conversations screen lists + selects + deletes) |
| Key storage | ✅ Android Keystore | ✅ iOS Keychain (`kSecClassGenericPassword`) |
| Tool calls (calendar / location / files / notify / share / etc.) | ✅ | ⛔ no agent tool loop on iOS yet |
| `ui_render` mini-apps + cached-tree replay | ✅ | ⛔ no agent |
| OAuth integrations (Linear, etc.) | ✅ Custom Tabs | ⛔ `StubOAuthGateway` returns `UserCancelled` |
| Voice input | ✅ Android `SpeechRecognizer` | ⛔ K/N 2.0.21 + iOS 26.4 SDK binding mismatch on `AVAudioSession.setActive` |
| Memory + trace persistence | ✅ Weft stores | ⛔ stubs (no agent to write into them) |
| Multi-agent declarations | ✅ | ⛔ single agent |

**Why iOS chat works without Weft:** rather than KMP-ifying the
Koog-dependent agent loop, `:composeApp/iosMain` ships a ~600 LOC
parallel Ktor stack — `AnthropicLlmClient` (SSE) +
`OpenAICompatLlmClient` (covers OpenAI / OpenRouter / DeepSeek) +
`IosAppStore` that routes per `state.activeProvider`.
See [The Koog blocker](#the-koog-blocker) for why we didn't port Weft.

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

**Strategy:** ship a minimal vertical slice and stub everything else
as "not on iOS yet." Don't try to port all of Weft. Each phase
below ships incremental value; stop at whichever phase is good
enough.

### Phase 0 — Xcode shell — DONE (commit `1d34743`)

`iosApp/iosApp.xcodeproj` with SwiftUI `@main App` calling
`InitKoinKt.doInitKoin()` + `MainViewControllerKt.MainViewController()`.
Run Script build phase invokes
`./gradlew :composeApp:linkDebugFrameworkIos{SimulatorArm64,Arm64}`
before Compile Sources; framework linked via the static-path
Framework Search Paths.

Info.plist sets `CADisableMinimumFrameDurationOnPhone` (CMP needs it
for ProMotion) plus `NSSpeechRecognitionUsageDescription` +
`NSMicrophoneUsageDescription` (preemptive for the eventual voice
work).

User Script Sandboxing must be **off** so Gradle can write outside
the Xcode sandbox.

### Phase 1 — Minimal viable chat — DONE (commits `7607d2d`, `2722dd5`, `77e1067`, `be4b439`)

Done in three slices:

1. **Boot + key persistence + one-shot Anthropic chat** (`7607d2d`):
   - `KeychainKeyVaultGateway` via `platform.Security.SecItem*` (`kSecClassGenericPassword`, accessible-after-first-unlock)
   - `KeyVaultGateway.getApiKey(provider)` added to the commonMain interface
   - One-shot `AnthropicClient` (Ktor + Darwin engine)
   - Real `IosAppStore` subscribing to `OnboardingRepository` / `ThemeRepository` / `ProviderPrefsRepository` — Resume / SetProvider / CompleteOnboarding / SubmitKey / SetPalette / SetThemeMode etc all flow through real persistence
   - iOS OS-bridge wiring (`onOpenUrl` → UIApplication, `onCopyText` → UIPasteboard, `onOpenAppDetailsSettings` → openSettingsURLString)
   - `ChatRoute` placeholder replaced with the commonMain `ChatScreen`

2. **Streaming + multi-provider + multi-conversation** (`2722dd5`):
   - `LlmClient` interface returning `Flow<LlmChunk>`
   - `AnthropicLlmClient` (SSE via manual `data:` line parsing)
   - `OpenAICompatLlmClient` (one impl for OpenAI / OpenRouter / DeepSeek — same wire format, different base URL + auth)
   - `IosAppStore` routes per `state.activeProvider`; streams chunks into `displayMessages` token-by-token
   - `Conversations.sq` schema added to `:data:sqldelight` (conversations + messages tables, transactional CRUD)
   - `IosConversationStoreGateway` (SQLDelight + sqldelight-coroutines-extensions)
   - `IosAppStore` tracks `currentConversationId`, persists every turn, hydrates on Resume + SelectConversation, autosets title from first user message
   - Drawer (☰) routes to the commonMain `ConversationsListScreen` for thread switching / deletion

3. **Persona wiring** (`77e1067`):
   - Subscribes to `PersonaRepository.activeVoice` / `activeRole`
   - Composes per-turn system prompt = base + voice instructions + role instructions (matches Weft's `extraVolatilePrefix` shape)
   - Chat header subtitle shows `<Model> · <PersonaLabel>`

4. **Polish** (`be4b439`):
   - SetProvider with no key routes to KeyPaste (mirrors Android UX)
   - Header shows the actual model name (`Claude Haiku 4.5`, not `Anthropic`)

**Voice — DEFERRED.** K/N 2.0.21's `platform.AVFAudio` bindings
against the iOS 26.4 SDK can't resolve `AVAudioSession.setActive`
(method exists in the .knm but the compiler reports "Unresolved
reference"). `IosSpeechGateway` is a documented stub with
`isAvailable = false`; mic CTA hides. Scaffolding lives in commit
`7607d2d` for pickup once Kotlin is upgraded.

### Phase 2 — Polish (mostly absorbed into Phase 1)

The "Phase 2" items in the original plan (streaming, conversation
persistence, OS bridges, multi-provider) all landed during Phase 1
because it was cheap to bundle them. Remaining Phase 2 items:

- **Per-provider model picker** — iOS currently pins one model per
  provider in the `LlmClient` factories. Wiring
  `ModelPrefsRepository` overrides into the clients would let the
  Providers screen drive Sonnet / Opus / GPT-5 / etc. selection.
- **Voice (when K/N upgrades)** — see deferred note above.
- **Title auto-improvement** — currently the first 40 chars of the
  user message. Could ask the model for a 3-5 word summary after
  the first exchange.

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

# KMP migration — current status

Living document. Updated each session. Pair with
[`kmp-migration-playbook.md`](kmp-migration-playbook.md) for the
per-feature recipe.

## Foundation — DONE

All seven foundation modules are migrated and verified to compile
on **both Android and iOS** targets. The gateway layer (commonMain
interfaces + Android impls + iOS stubs) landed in the follow-up
session, so every cross-cutting Weft dependency now has a
KMP-friendly contract.

| Module | What's in it |
|---|---|
| ✅ `:core:model` | AppEffect, ThemeMode, ThemePrefs, AppPalette, **ProviderKind**, **ModelTier**, **Persona / PersonaKind / BuiltInPersonas**, **MiniApp** |
| ✅ `:core:design-system` | UndercurrentColors, UndercurrentShapes, UndercurrentTypography, UndercurrentTheme, Palettes (8 color tables + `AppPalette.colors(dark)` extension) |
| ✅ `:core:ui` | LoadingPlaceholder, ScreenScaffold, ScaffoldTextAction, TokenDivider, SectionLabel |
| ✅ `:shared` | **AgentEngine** + ChatChunk/AgentState/StubAgentEngine. **Gateway layer**: KeyVaultGateway, OAuthGateway, ConversationStoreGateway, MemoryStoreGateway, TraceStoreGateway, UsageGateway, ModelCatalog, SpeechGateway, UiBridgeGateway (with mirror types — ConversationSummary, MemoryEntry/Scope, AgentTrace/LlmCallTrace/ToolCallTrace + status enums, UsageTotals, ModelInfo/ModelPool, VoiceState, ComponentNode/UiRenderEvent, OAuthConfig/OAuthTokens/OAuthResult). iOS stubs for all nine. |
| ✅ `:data:weft` (Android-only) | WeftAgentEngine + **nine gateway impls** — WeftKeyVaultGateway, WeftOAuthGateway, WeftConversationStoreGateway, WeftMemoryStoreGateway, WeftTraceStoreGateway, WeftUsageGateway, WeftModelCatalog, AndroidSpeechGateway, WeftUiBridgeGateway. |
| ✅ `:data:datastore` | createPreferencesDataStore (KMP factory + Android/iOS actuals), ThemeRepository, OnboardingRepository, PersonaRepository, IntegrationsRepository, MiniAppsRepository, ProviderPrefsRepository, ModelPrefsRepository — **7 repos total** |
| ✅ `:data:sqldelight` | UndercurrentDatabase (Records.sq schema), expect class DatabaseDriverFactory + Android (AndroidSqliteDriver) / iOS (NativeSqliteDriver) actuals |

## Features — 5 of 17 done

| Feature | Status | Blocker / notes |
|---|---|---|
| ✅ `:feature:settings` | **DONE** | Proof-of-pattern. SettingsScreen consumes ProviderKind from :core:model + ScreenScaffold from :core:ui. |
| ✅ `:feature:conversations` | **DONE** | VM + ConversationsListScreen + ConversationGrouping in commonMain. Date bucketing now uses kotlinx.datetime (was `java.util.Calendar`/`SimpleDateFormat`). |
| ✅ `:feature:memories` | **DONE** | VM + AgentMemoriesScreen in commonMain via MemoryStoreGateway. Manual kotlinx.datetime "MMM d · HH:mm" timestamp. |
| ✅ `:feature:traces` | **DONE** | VM + TraceViewerScreen (full detail view + clipboard formatters) in commonMain via TraceStoreGateway. `String.format` swapped for a manual one-decimal helper. |
| ✅ `:feature:usage` | **DONE** | VM + UsageScreen in commonMain via UsageGateway. `java.time.LocalDate.now()` → kotlinx.datetime; per-decimal `String.format` swapped for a manual helper. |
| ⏳ `:feature:onboarding` | Repo migrated; Screen pending | OnboardingScreen depends on BuiltInPersonas (✅ in :core:model) + ProviderKind (✅ in :core:model) + ModelCatalog (✅ in :shared). |
| ⏳ `:feature:theme` | DataStore repo migrated; Screen pending | AppearanceScreen needs migration — likely simple. |
| ⏳ `:feature:personas` | Repo + types migrated; Screen + VM pending | PersonasScreen is 544 LOC. ViewModel directly mutates PersonaRepository — straightforward. |
| ⏳ `:feature:miniapps` | Repo + MiniApp type migrated; Screen + VM pending | Screen consumes UIUpdate (Weft type) for the cached-render plumbing. UiBridgeGateway (✅ in :shared) covers it. |
| ⏳ `:feature:integrations` | Repo migrated; Screen + VM pending | OAuthGateway (✅ in :shared) covers it. |
| ⏳ `:feature:providers` | Both repos migrated; Screen + VM pending | Largest non-chat screen (717 LOC). ModelCatalog (✅) covers reads; still needs a provider-rebuild trigger gateway for `WeftRuntime.buildExecutorFor`. |
| ⏳ `:feature:keypaste` | — | KeyVaultGateway (✅) covers it. |
| ⏳ `:feature:voice` | — | SpeechGateway (✅) covers it. |
| ⏳ `:feature:maps` | — | Pure Compose around a map URL string + Weft Location capability. Light. |
| ⏳ `:feature:creator` | — | Uses agent tools (create_persona, create_mini_app). Routes through AgentEngine — manageable. |
| ⏳ `:feature:navigation` | — | Cross-cutting. Migrate alongside :core:navigation. |
| ⏳ `:feature:chat` | — | **The big one** — 1387 LOC. Streaming UI + tool-call rendering + agent state. Consumes AgentEngine (already defined). Last to migrate per playbook. |

## Patterns established

Every remaining feature follows one of these recipes:

### Recipe A — feature with Weft-store reads only
Examples: `:feature:conversations`, `:feature:memories`, `:feature:traces`, `:feature:usage`

1. Define a `<Feature>Gateway` interface in `:shared/commonMain/.../gateway/` exposing the read operations the feature needs (list/get/delete/observe). Use KMP-friendly types from :core:model or local mirrors.
2. Add an Android implementation in `:data:weft/src/main/kotlin/.../` wrapping the corresponding Weft store (`runtime.conversationStore`, `runtime.memoryStore`, etc.).
3. Add an iOS stub in `:shared/iosMain/.../gateway/` that returns empty flows / throws on writes.
4. Move the feature's `<Feature>ViewModel` to `:feature:<name>/commonMain` — constructor takes the gateway interface.
5. Move the `<Feature>Screen` to `:feature:<name>/commonMain` — replace `internal` with `public`, swap any Weft imports for the gateway types.

### Recipe B — feature with DataStore prefs only
Examples: `:feature:theme`, `:feature:onboarding`, parts of `:feature:miniapps` and `:feature:personas`

1. Repository already migrated to `:data:datastore/commonMain`. Pull the ViewModel + Screen forward.
2. Constructor takes the migrated `<Name>Repository` directly. No gateway needed.
3. Move Screen + ViewModel to `:feature:<name>/commonMain`.

### Recipe C — feature with OAuth / KeyVault / agent tools
Examples: `:feature:integrations`, `:feature:keypaste`, `:feature:creator`, `:feature:voice`

1. Define an interface in `:shared` for the cross-cutting concern (`OAuthGateway`, `KeyVaultGateway`, `SpeechGateway`).
2. Android impl in `:data:weft` delegates to the Weft contract.
3. iOS stub in `:shared/iosMain` returns "not supported".
4. Migrate ViewModel + Screen consuming the interface.

### Recipe D — `:feature:chat`
The single hardest one. Bigger writeup needed before tackling it.
Consumes AgentEngine (already defined) but also needs UIUpdate
plumbing (the `ui_render` payload pipeline) and the streaming
chunk → UI translation. Plan a focused session.

## Cross-feature gateway layer — DONE

All nine cross-cutting interfaces are defined in `:shared/commonMain`
with mirror types, backed by Android impls in `:data:weft`, and
stubbed in `:shared/iosMain`. Both targets compile.

| Interface | Used by | Android impl |
|---|---|---|
| `KeyVaultGateway` | keypaste, providers | `WeftKeyVaultGateway` (delegates to Weft `KeyVault` + provider-alias map) |
| `OAuthGateway` | integrations | `WeftOAuthGateway` (wraps `OAuthClient` + `OAuthTokenStore`, translates `OAuthResult` sealed variants) |
| `ConversationStoreGateway` | conversations, chat | `WeftConversationStoreGateway` (search/delete/clear-all over `ConversationStore`) |
| `MemoryStoreGateway` | memories, chat | `WeftMemoryStoreGateway` (read-only flow + delete/clear over `MemoryStore`) |
| `TraceStoreGateway` | traces | `WeftTraceStoreGateway` (full mirror of AgentTrace/LlmCallTrace/ToolCallTrace + setFeedback) |
| `UsageGateway` | usage, providers | `WeftUsageGateway` (totals flow over `UsageStore`) |
| `ModelCatalog` | providers, onboarding | `WeftModelCatalog` (projects Koog `LLModel` → lossy `ModelInfo`; preserves `hasVision` / `hasTools` capability bits) |
| `SpeechGateway` | voice | `AndroidSpeechGateway` (wraps `SpeechRecognizer` directly — no Weft dependency) |
| `UiBridgeGateway` | miniapps, chat | `WeftUiBridgeGateway` (`snapshotFlow` over `ComposeUiBridge.lastUpdate`, filters to RenderTree events) |

Mirror types live alongside their gateway in `shared/commonMain/.../gateway/`.
Wire format compatible with the Weft originals — cached `ui_render`
JSON round-trips cleanly between the mirror `ComponentNode` and
`dev.weft.contracts.ComponentNode`.

## Recommended next session

1. **Recipe B features** (theme, onboarding, personas, miniapps). ~½ day.
2. **Recipe C features** (integrations, keypaste, voice, creator, providers, maps). ~1 day total.
3. **`:feature:chat` last** — needs its own focused session. ~1 day.
4. **`:androidApp` + `:composeApp` wiring** — Koin DI module that picks the platform-correct implementation of every gateway, the top-level App composable, screen routing, MainActivity. ~1 day.
5. **iOS shell** — Xcode project, SwiftUI scene hosting ComposeApp.framework. ~½ day.
6. **Delete `app/`** — once everything builds through the new modules.

Estimated total remaining: ~4 focused days.

## Patterns learned from Recipe A

- **`:shared` brings `:core:model` transitively.** Set up in the gateway commit; feature modules add `implementation(projects.shared)` and pick up `ProviderKind` / `ModelTier` automatically.
- **`org.koin.compose.viewmodel.koinViewModel`** is the commonMain entry point. Don't import `org.koin.androidx.compose.koinViewModel` — that's the Android-only shim from the old app.
- **`androidx.lifecycle.ViewModel` + `viewModelScope`** are KMP-available through koin-compose-viewmodel's transitive deps. No special imports needed.
- **`java.util.Date` / `SimpleDateFormat` / `java.text.*` / `java.time.*`** → kotlinx.datetime + `kotlin.time.Clock` per CLAUDE.md note. Add `implementation(libs.kotlinx.datetime)` per feature, opt-in via `@OptIn(ExperimentalTime::class)` when using `kotlin.time.Instant`.
- **`String.format("%.Nf", v)` doesn't exist in commonMain stdlib.** Write a small `formatDecimal(v, n)` helper (see `feature/usage`). One-decimal-only variants also acceptable inline.
- **`Map.toSortedMap()` doesn't exist in commonMain stdlib.** Use `entries.sortedBy { it.key }` (ISO dates sort lexicographically = chronologically).

## What's solid right now

- All seven foundation modules compile clean on both Android + iOS.
- The Weft composite build resolves end-to-end through
  `includeBuild("../weft")`.
- Convention plugins (build-logic/) are stable; adding new feature
  modules is a one-line plugin application + a copy-paste of the
  feature dir structure.
- DataStore + SQLDelight + Compose Multiplatform + Koin + Kotlin
  Time all work as expected in commonMain.
- One feature migrated end-to-end proves the pattern works.

## What's stale / paused

- `app/` is no longer in `settings.gradle.kts` and isn't building.
  Its source is reference-only until the migration finishes and it
  gets deleted.
- The `:androidApp` and `:composeApp` modules are scaffold-only
  (empty `commonMain/`). Nothing runs end-to-end yet on Android —
  only individual module compiles are green.
- `iosApp/` isn't created. No iOS Xcode project until `:composeApp`
  has real content.

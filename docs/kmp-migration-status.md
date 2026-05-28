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

## Features — 15 of 17 done

| Feature | Status | Blocker / notes |
|---|---|---|
| ✅ `:feature:settings` | **DONE** | Proof-of-pattern. |
| ✅ `:feature:conversations` | **DONE** | Recipe A. |
| ✅ `:feature:memories` | **DONE** | Recipe A. |
| ✅ `:feature:traces` | **DONE** | Recipe A. Full detail view + clipboard formatters. |
| ✅ `:feature:usage` | **DONE** | Recipe A. |
| ✅ `:feature:theme` | **DONE** | Stateless AppearanceScreen. |
| ✅ `:feature:onboarding` | **DONE** | `catalogFor(provider)` lifted to `modelCountFor` lambda. |
| ✅ `:feature:personas` | **DONE** | `CreatorKind` dep replaced with `(PersonaKind) -> Unit` lambda. |
| ✅ `:feature:miniapps` | **DONE** | `TreeRenderer` hoisted to `treePreview` lambda. |
| ✅ `:feature:integrations` | **DONE** | OAuthGateway + IntegrationsRepository. Integration mirror uses commonMain OAuthConfig. |
| ✅ `:feature:keypaste` | **DONE** | New `KeyValidationGateway` added; pure-data helpers (`apiConsoleUrl`, `signupHint`, `hostName`, `keyPlaceholder`) moved to `:core:model`. `openInBrowser` lifted to `onOpenConsole` lambda. Material icons replaced with "Show"/"Hide" labels. |
| ✅ `:feature:voice` | **DONE** | WaveformBars in commonMain reading from `SpeechGateway.rmsdB`. Old Android-only VoiceRecognizer replaced by AndroidSpeechGateway (landed with gateway batch). |
| ✅ `:feature:maps` | **DONE** | No UI to migrate — feature is just `ShowLocationOnMapTool` (a Weft tool). Moved to `:data:weft/.../tools/` since Weft tools must be androidMain. |
| ✅ `:feature:creator` | **DONE** | CreatorSession + CreatorKind + CreatorScreen in commonMain. Tree-rendering body hoisted to a `body: @Composable () -> Unit` lambda so host can wire Weft's TreeRenderer + ComposeUiBridge. `CreatorTools` (create_persona, create_mini_app) stays in `app/` for now — depends on un-migrated `Screen` / `NavigationChannel`; moves to `:data:weft` when navigation lands. |
| ✅ `:feature:providers` | **DONE** | Largest screen yet (~720 LOC). Switched from Koog `LLModel` to `ModelInfo` mirror; from `catalogFor`/`defaultPoolFor` to `ModelCatalog` gateway; from inline `validateKey` to `KeyValidationGateway`; from CCT `openInBrowser` to `onOpenConsole` lambda. `TipBox` promoted to `:core:ui`. Material icons (Visibility, ArrowDropDown, KeyboardArrowRight) → Unicode glyphs. |
| ⏳ `:feature:navigation` | — | Cross-cutting. Migrate alongside `:core:navigation`. |
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

1. **`:feature:chat`** — the big one. ~1 day. Streaming UI + tool-call rendering. Already has AgentEngine + UiBridgeGateway. The tree-rendering body needs hoisting (same pattern as miniapps + creator).
2. **`:androidApp` + `:composeApp` wiring** — Koin DI module that picks the platform-correct gateway impl, the top-level App composable, screen routing, MainActivity. Also: `CreatorTools` (`create_persona`, `create_mini_app`) gets moved to `:data:weft` once `:feature:navigation` lands. ~1 day.
3. **iOS shell** — Xcode project, SwiftUI scene hosting ComposeApp.framework. ~½ day.
4. **Delete `app/`** — once everything builds through the new modules.

Estimated total remaining: ~2.5 focused days.

## Patterns learned (Recipes A + B + C)

- **`:shared` brings `:core:model` transitively.** Set up in the gateway commit; feature modules add `implementation(projects.shared)` and pick up `ProviderKind` / `ModelTier` automatically.
- **`org.koin.compose.viewmodel.koinViewModel`** is the commonMain entry point. Don't import `org.koin.androidx.compose.koinViewModel` — that's the Android-only shim from the old app.
- **`androidx.lifecycle.ViewModel` + `viewModelScope`** are KMP-available through koin-compose-viewmodel's transitive deps. No special imports needed.
- **`java.util.Date` / `SimpleDateFormat` / `java.text.*` / `java.time.*`** → kotlinx.datetime + `kotlin.time.Clock` per CLAUDE.md note. Add `implementation(libs.kotlinx.datetime)` per feature, opt-in via `@OptIn(ExperimentalTime::class)` when using `kotlin.time.Instant`.
- **`String.format("%.Nf", v)` doesn't exist in commonMain stdlib.** Write a small `formatDecimal(v, n)` helper (see `feature/usage`). One-decimal-only variants also acceptable inline.
- **`Map.toSortedMap()` doesn't exist in commonMain stdlib.** Use `entries.sortedBy { it.key }` (ISO dates sort lexicographically = chronologically).
- **Lift Android-only Composable dependencies to lambda parameters** (Recipe B insight). When a screen needs an Android-only Composable like `TreeRenderer` or a static Android-only call like `catalogFor()`, take the rendering / lookup as a constructor lambda. The host wires it from the Android nav glue; the screen stays commonMain. See `feature/miniapps` (`treePreview`) + `feature/onboarding` (`modelCountFor`).
- **When a screen depends on a sibling feature module that hasn't migrated yet** (e.g. `:feature:personas` used `:feature:creator`'s `CreatorKind`), lift to a generic callback — `(PersonaKind) -> Unit` instead of `(CreatorKind) -> Unit`. The host translates. Avoids cross-feature ordering constraints.
- **Material `icons-extended` isn't in CMP commonMain by default.** Replace with Unicode glyphs: `▾` (ArrowDropDown), `›` (KeyboardArrowRight), `←` (back), `×` (Close), plus plain "Show"/"Hide" text for password toggles. Matches the design-language conventions of the existing migrated screens.
- **Promote shared screen helpers to `:core:ui` lazily — when the second feature would import them.** `TipBox` lived in `app/ui/` until `:feature:providers` migration needed it; promoted then. Same approach if `:feature:chat` needs anything more.
- **Pure-data per-provider helpers** (`apiConsoleUrl`, `signupHint`, `hostName`, `keyPlaceholder`) belong in `:core:model` as `ProviderKind` extensions — both `:feature:keypaste` and `:feature:providers` consume them.

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

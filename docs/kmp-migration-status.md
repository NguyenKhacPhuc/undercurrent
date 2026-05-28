# KMP migration — current status

Living document. Updated each session. Pair with
[`kmp-migration-playbook.md`](kmp-migration-playbook.md) for the
per-feature recipe.

## Foundation — DONE

All seven foundation modules are migrated and verified to compile
on **both Android and iOS** targets.

| Module | What's in it |
|---|---|
| ✅ `:core:model` | AppEffect, ThemeMode, ThemePrefs, AppPalette, **ProviderKind**, **ModelTier**, **Persona / PersonaKind / BuiltInPersonas**, **MiniApp** |
| ✅ `:core:design-system` | UndercurrentColors, UndercurrentShapes, UndercurrentTypography, UndercurrentTheme, Palettes (8 color tables + `AppPalette.colors(dark)` extension) |
| ✅ `:core:ui` | LoadingPlaceholder, ScreenScaffold, ScaffoldTextAction, TokenDivider, SectionLabel |
| ✅ `:shared` | **AgentEngine interface**, ChatChunk, AgentState, StubAgentEngine (iosMain) |
| ✅ `:data:weft` (Android-only) | WeftAgentEngine — bridges WeftAgent → AgentEngine |
| ✅ `:data:datastore` | createPreferencesDataStore (KMP factory + Android/iOS actuals), ThemeRepository, OnboardingRepository, PersonaRepository, IntegrationsRepository, MiniAppsRepository, ProviderPrefsRepository, ModelPrefsRepository — **7 repos total** |
| ✅ `:data:sqldelight` | UndercurrentDatabase (Records.sq schema), expect class DatabaseDriverFactory + Android (AndroidSqliteDriver) / iOS (NativeSqliteDriver) actuals |

## Features — 1 of 17 done

| Feature | Status | Blocker / notes |
|---|---|---|
| ✅ `:feature:settings` | **DONE — both targets compile** | Used as the proof-of-pattern. SettingsScreen consumes ProviderKind from :core:model + ScreenScaffold from :core:ui. |
| ⏳ `:feature:onboarding` | Repo migrated; Screen pending | OnboardingScreen depends on BuiltInPersonas (✅ in :core:model) + ProviderKind (✅ in :core:model) + `dev.weft.android.routing.catalogFor` (NEEDS GATEWAY: ModelCatalog). |
| ⏳ `:feature:theme` | DataStore repo migrated; Screen pending | AppearanceScreen needs migration — likely simple. |
| ⏳ `:feature:personas` | Repo + types migrated; Screen + VM pending | PersonasScreen is 544 LOC. ViewModel directly mutates PersonaRepository — straightforward. |
| ⏳ `:feature:miniapps` | Repo + MiniApp type migrated; Screen + VM pending | Screen consumes UIUpdate (Weft type) for the cached-render plumbing. Needs `UiBridgeGateway`. |
| ⏳ `:feature:integrations` | Repo migrated; Screen + VM pending | ViewModel uses dev.weft.oauth.{OAuthClient, OAuthResult, OAuthTokenStore}. Needs `OAuthGateway` in :shared + :data:weft impl. |
| ⏳ `:feature:providers` | Both repos migrated; Screen + VM pending | Largest non-chat screen (717 LOC). VM calls WeftRuntime.buildExecutorFor + model catalog. Needs `ModelCatalog` + provider-rebuild trigger gateway. |
| ⏳ `:feature:keypaste` | — | Uses Weft KeyVault for key persistence. Needs `KeyVaultGateway`. |
| ⏳ `:feature:conversations` | — | Uses Weft ConversationStore directly. Needs `ConversationStoreGateway` (read-only queries — list/select/delete). |
| ⏳ `:feature:memories` | — | Uses Weft MemoryStore. Needs `MemoryStoreGateway`. |
| ⏳ `:feature:traces` | — | Uses Weft TraceStore + AgentTrace + UsageStore. Needs `TraceStoreGateway`. |
| ⏳ `:feature:voice` | — | Uses Weft Speech capability. Add `SpeechGateway` OR route through AgentEngine. |
| ⏳ `:feature:maps` | — | Pure Compose around a map URL string + Weft Location capability. Light. |
| ⏳ `:feature:creator` | — | Uses agent tools (create_persona, create_mini_app). Routes through AgentEngine — manageable. |
| ⏳ `:feature:usage` | — | Uses `dev.weft.harness.cost.UsageTotals`. Needs `UsageGateway`. Also uses java.time.LocalDate (swap to kotlinx.datetime). |
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

## Required cross-feature work

Before the remaining 16 features can all land, these abstractions
need to exist in `:shared` + `:data:weft`:

| Interface | Used by |
|---|---|
| `KeyVaultGateway` | keypaste, providers |
| `OAuthGateway` | integrations |
| `ConversationStoreGateway` | conversations, chat |
| `MemoryStoreGateway` | memories, chat |
| `TraceStoreGateway` | traces |
| `UsageGateway` | usage, providers |
| `ModelCatalog` | providers, onboarding |
| `SpeechGateway` | voice |
| `UiBridgeGateway` | miniapps, chat (`ui_render` plumbing) |

Plus a mirror of `UIUpdate` in `:core:model` if features (notably
miniapps + chat) need to type their state with it.

## Recommended next session

1. **Add the nine gateway interfaces** in `:shared` + Android impls in `:data:weft`. ~1 day of focused work.
2. **Then bulk-migrate Recipe A features** (conversations, memories, traces, usage) — they're mechanically similar once their gateway exists. ~½ day for all four.
3. **Recipe B features next** (theme, onboarding, personas, miniapps without UIUpdate) — ~½ day.
4. **Recipe C features** (integrations, keypaste, voice, creator, providers, maps) — ~1 day total.
5. **`:feature:chat` last** — needs its own focused session. ~1 day.
6. **`:androidApp` + `:composeApp` wiring** — Koin DI module that picks the platform-correct implementation of every gateway, the top-level App composable, screen routing, MainActivity. ~1 day.
7. **iOS shell** — Xcode project, SwiftUI scene hosting ComposeApp.framework. ~½ day.
8. **Delete `app/`** — once everything builds through the new modules.

Estimated total remaining: ~5-6 focused days.

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

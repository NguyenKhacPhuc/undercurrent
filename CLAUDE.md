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
# Debug APK.
./gradlew :app:assembleDebug

# Just the Kotlin compile (faster when iterating).
./gradlew :app:compileDebugKotlin

# Install + start + tail logs (most useful combo).
adb shell am force-stop dev.weft.undercurrent
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n dev.weft.undercurrent/.core.MainActivity
adb logcat | grep -E "Undercurrent|YourTag"
```

**Force-stop is important** when iterating on tools. The
`WeftRuntime`'s tool catalog is built once at process start; reinstalling
without killing the old process means the agent keeps using the stale
catalog.

## Architecture

MVI with Koin DI.

- `core/AppStore.kt` — root `ViewModel`. Holds the `WeftAgent`, the
  conversation list, and the screen state machine. All state mutations
  go through `dispatch(AppIntent.X)` and update a `MutableStateFlow<AppState>`.
- `core/AppState.kt` — every screen's state lives here. `Screen` sealed
  interface enumerates navigable destinations. `previousScreen` is
  captured by the `Navigate` reducer so back-button routing works for
  screens with multiple entry points (e.g. Integrations).
- `core/AppIntent.kt` — every action the user / agent can take.
- `core/MainActivity.kt` — Compose host. Renders one screen at a time
  based on `state.screen`. Also handles the OAuth callback deep link
  (`undercurrent://oauth/…`) and the process restart for MCP changes.
- `di/AppModule.kt` — Koin wiring. **Single source of truth** for
  which tools the agent has, which OAuth tokens exist, which MCP
  servers are wired up.
- `features/<feature>/` — one directory per surface (chat, personas,
  integrations, providers, …). Each contains a Screen, a ViewModel
  (if needed), a Repository (if persistent), and any feature-specific
  tools.

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

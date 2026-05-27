# Undercurrent

A small, private personal-assistant Android app. Conversations live on
the device; you bring your own API key; the model can call native
tools, render real Compose UI, and remember things you tell it.

Built on the [Weft][weft] substrate as the reference app for the
SDK's full surface area: streaming chat, multi-provider key management,
custom personas, custom agent tools, memory browsing, conversation
search, trace export, and palette theming.

## Status

Pre-1.0. Tracks the Weft SDK closely — both repos evolve together.

## Features

- **Streaming chat** with persistent conversations and full-text search.
- **Multi-provider** key management — Anthropic, OpenAI, OpenRouter,
  DeepSeek. Per-provider per-tier model overrides.
- **Personas** — pick a voice (Editor, Field Notes, Reader, Almanac,
  Default) and an optional role (Developer, Doctor, Lawyer, Teacher,
  Researcher), or write your own. The agent picks them up per turn.
- **Theme tools** — ask the model to switch palette ("try newsprint",
  "make it dark") and the UI recolors instantly via the same DataStore
  flow that backs Settings.
- **Memory browser** — every fact the agent stored about you is
  inspectable + deletable.
- **Traces + cost tracking** — every turn captures a redactable trace
  you can export, with per-provider token + dollar accounting.

## Build

This app depends on the Weft SDK via Gradle composite build, so both
repos need to be checked out side-by-side.

```bash
# Pick a parent directory. Both repos must be siblings inside it.
mkdir mas && cd mas

# The SDK. Clone with the directory name `weft` — `settings.gradle.kts`
# resolves `includeBuild("../weft")`.
git clone https://github.com/NguyenKhacPhuc/android-harness.git weft

# The app.
git clone https://github.com/NguyenKhacPhuc/undercurrent.git

cd undercurrent
./gradlew :androidApp:assembleDebug
```

Open the project in Android Studio (Hedgehog or newer). Run on a device
with `minSdk` 26 or higher. First launch lands on an onboarding flow
that walks you through picking a provider, pasting a key, and choosing
a starting voice.

[weft]: https://github.com/NguyenKhacPhuc/android-harness

## Stack

- Android (`compileSdk` 35, `minSdk` 26).
- Kotlin 2.x with Jetpack Compose.
- [Weft SDK][weft] for the agent loop, tool catalog, persistence, and
  Compose-component framework.
- Koin for DI; AndroidX DataStore for prefs; SQLDelight (transitively
  via Weft) for conversation + memory persistence.

## Layout

```
app/src/main/kotlin/dev/weft/undercurrent/
├── core/          MVI store, App composable, root navigation
├── di/            Koin module — single source of wiring
├── theme/         Palettes, modes, ThemeRepository
├── features/
│   ├── chat/      ChatScreen + skill registry + DisplayMessage
│   ├── onboarding/ First-launch flow (provider + persona picker)
│   ├── personas/  Voice + role selection, custom-persona editor
│   ├── providers/ Provider switching, key vault, per-tier overrides
│   ├── conversations/ Drawer + full-screen conversation list
│   ├── memories/  Browseable agent memory store
│   ├── traces/    Per-turn trace viewer + JSON export
│   ├── settings/  Settings hub + Appearance sub-screen
│   ├── usage/     Cost + token usage history
│   └── theme/     Agent-callable tools: set_theme_palette, set_theme_mode
└── ui/            Reusable surfaces — AppDrawer, ScreenScaffold
```

## License

Not yet licensed — treat this repo as source-available reference code
until a `LICENSE` file lands. The Weft SDK it depends on is Apache 2.0.

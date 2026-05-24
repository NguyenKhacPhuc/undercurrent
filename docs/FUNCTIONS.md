# Undercurrent — Functions Summary

What this app actually does, feature by feature. Reference for designers, PM, or anyone scoping work.

---

## What it is

A **private, on-device AI assistant** for Android. The user picks a model provider (Anthropic, OpenAI, OpenRouter, DeepSeek), pastes an API key, and chats with an agent that can use tools, remember facts about them, route to the right model per request, and render generated UI inline. Everything except the live API call lives on the device — conversations, memories, traces, cost data, keys.

Think: ChatGPT app, but the user supplies the key, owns the data, and can swap providers / personas / models.

---

## Screens

### 1. Loading
Boot-time placeholder. The app reads the user's stored key + persona prefs, instantiates the agent, hydrates the most recent conversation, then transitions away. No user input.

### 2. Onboarding (first-launch only, 3 pages)
- **"Hi."** — Greeting. Establishes voice ("Personal & warm" — first-person AI introducing itself).
- **"Your data stays here."** — Privacy promise. Reassures the user nothing's uploaded.
- **"One last thing."** — Pick a model provider (2×2 grid of cards: Anthropic, OpenAI, OpenRouter, DeepSeek). "Let's go →" advances to the key paste screen.

Page indicator at the top, "Skip" affordance on the middle screen, primary CTA in the lower-right corner.

### 3. Key Paste
Shown after onboarding, or any time the user has no key stored for their active provider. One text field (masked, password-style), one Save button. Validates the key against the provider's API before persisting — invalid keys never reach the vault.

### 4. Chat (main surface)
- **Header**: drawer button (≡), thread title (auto-derived from the conversation's first user message), "New" button.
- **Message list**: user messages in muted gray surface, assistant messages in document-feel serif body with full markdown rendering (headings, lists, links, inline + block code, bold/italic). Tool invocations show as inline chips with status (running/done/failed). Streaming cursor blinks under the in-progress assistant reply.
- **Bottom input row**: tier chip (Auto / Cheap / Standard / Heavy), expanding text field, mic button (push-and-hold for voice with live waveform), send button.
- **Message actions**: tap an assistant message to reveal Copy + Regenerate.
- **Skill shortcuts**: typing a slash command (e.g. `/clear`, `/memories`) bypasses the LLM and runs locally — instant, no token spend.
- **Inline notifications**: in-app permission banner when notifications are blocked, degraded-mode banner when the model circuit-breaker is tripped.

### 5. Side Drawer (slide from left on Chat)
Navigation hub. Lists recent conversations (today / yesterday / earlier / older), then nav links:
- New chat
- All conversations →
- Personas →
- Memories →
- Traces →
- Settings →

### 6. Conversations List
Full list of every persisted thread, grouped by recency. Search bar at the top (token-styled like a chat input — full-text across titles + messages). Tap to resume; small inline Delete on each row (confirm dialog). "Clear all" at the bottom.

### 7. Personas
Persona picker. Active persona injects extra system-prompt text into every turn (tone, style, role).
- **Built-in**: 5 curated personas (Default, plus 4 others) — always present, can't delete.
- **Custom**: user-created. "+ New" opens an Add dialog (Name, Tagline, Instructions textarea). Newly-created persona is auto-selected.
- Tapping any row switches the active persona immediately — the next assistant turn picks it up.

### 8. Memories
Lists every fact the agent has saved about the user (via the `memory_save` tool). Each row shows scope chip + content + Delete action. "Clear all" in the header. Accountability gate — anything not visible here, the substrate isn't remembering.

### 9. Traces
Debug surface. Lists recent agent turns with status + duration + token count. Tap a row to drill into the detail screen:
- Feedback row (👍 / 👎 — persisted with the trace)
- Meta block (conversation id, started, duration, tokens)
- User message + final assistant reply
- LLM calls (model, duration, tokens per call)
- Tool calls (name, args preview, result preview, error if failed)
- **Export** button — redacts → saves to a file → opens system share sheet.

### 10. Usage Dashboard (entered from Settings)
- Hero: today's spend + lifetime spend, side-by-side.
- Token breakdown: input / output / cache reads / cache writes.
- Cache savings callout (when there have been cache reads).
- Bar chart: last 14 days of daily spend.
- Footer: last model used.

### 11. Settings (index)
3 rows, each drilling into a sub-screen:
- **Provider** → ProvidersScreen (subtitle shows active provider, e.g. "Anthropic")
- **Appearance** → AppearanceScreen (subtitle shows current palette + mode, e.g. "Vellum · Auto")
- **Usage** → UsageScreen ("Tokens and cost")

### 12. Provider Settings (sub-screen of Settings)
- **Providers list**: one card per backend (Anthropic / OpenAI / OpenRouter / DeepSeek). Tap to select (becomes active). Each card expands to reveal:
  - API key field (masked, password input) + Save (validates first) / Remove key actions.
  - Models customization (collapsed; tap to expand): four rows for Cheap / Standard / Vision / Heavy tier, each a dropdown of the provider's model catalog with capability notes ("no vision", "no tools — limited agent use").
- **Default model tier**: segmented control (Auto / Cheap / Standard / Heavy) + a TipBox explaining tiers.

### 13. Appearance (sub-screen of Settings)
- **Theme**: 4 palette cards (Warm dark, Sage & ochre, Newsprint, Vellum). Each card shows live color swatches (bg / ink / accent dots) + name + tagline. Active palette gets accent border.
- **Mode**: Auto / Light / Dark segmented control.

### 14. Rendered Tree (agent-generated UI)
When the agent calls a UI-rendering tool (e.g. "show a form to collect X"), the app navigates here and renders the agent-defined component tree. The user fills the form / taps buttons; results stream back into the chat history.

---

## Cross-cutting features

### Multi-provider with hot-swap
User picks one provider at a time, but keys + per-provider model overrides are stored independently. Switching providers in Settings rebuilds the agent on the fly — next message goes to the new backend.

### Per-tier model routing
The SDK exposes 4 "tiers" (Cheap / Standard / Vision / Heavy). Each provider has a default model for each tier. The user can override any slot. The router picks a tier per-turn based on the request, OR the user pins a default tier in Settings, OR overrides per-message via the chat-input tier chip.

### Voice input
Push-and-hold mic button in chat input. Live RMS waveform during recording. Releases → SpeechRecognizer transcribes → text drops into the input field for review before send. Falls back to a permission request the first time.

### Markdown rendering
Hand-rolled (not a library): paragraphs, ATX headings, bullet/numbered lists, fenced + inline code, bold/italic, links via Chrome Custom Tabs. Streaming-safe — re-parses on every text delta without flickering.

### Streaming
Every send → SSE stream of `StreamChunk`s (TextDelta / ToolStarting / ToolCompleted / ToolFailed / Done / Failed). UI appends deltas to the in-progress bubble in real time; cursor blinks until Done.

### True regenerate
Regenerate on the last assistant message rolls back conversation history + persistence to just-after-the-last-user-message, then re-streams a fresh reply. No leftover duplicate replies.

### Conversation persistence
SQLite-backed. Every turn (user message + assistant reply + tool calls) is persisted; conversations get auto-generated titles. Resume any thread from the Conversations list or drawer.

### Memory store
Agent has a `memory_save` tool. Saved memories are user-visible in the Memories screen and individually deletable. Persisted to SQLite.

### Cost tracking
Every LLM call updates a UsageStore (tokens in / out / cached, USD cost per model). Powers the Usage dashboard. No external service — all local.

### Traces & feedback
Every agent turn produces a trace: status, duration, LLM calls + tool calls with previews. User can thumbs-up/down for QA, export as JSON (redacted) for sharing.

### Theme system
4 palettes × (light / dark) = 8 color schemes. User picks palette + mode independently (Auto follows system). Switching crossfades the whole UI. Default is Vellum (sepia, no chromatic accent — "pure writing surface").

### Key vault
API keys encrypted on-device (Android Keystore-backed via androidx.security). Keys never leave the device except in the Authorization header of API calls. Masked display in Settings as `•••• last4`.

### Onboarding gate
First launch routes through the 3-page onboarding. Subsequent launches resume to the last conversation. If the user's active provider has no stored key, they land on Key Paste instead.

### Notifications
Optional. The agent can schedule reminders via a `schedule_create` tool. If the user denies the runtime permission, an inline banner on Chat prompts them to enable it from settings.

### DevTools (debug builds only)
Floating FAB → bottom sheet with live runtime inspection (active model, in-flight calls, etc.). Stripped from release builds.

---

## Architecture (relevant to design decisions)

- **Single Activity** (`MainActivity`) with state-driven screen switching — no Navigation Compose / Fragment routing. State change = screen change. Designers can think in terms of states, not transitions.
- **Edge-to-edge** rendering with `safeDrawingPadding` — content sits inside safe area but background paints all the way to the bezel.
- **No bottom nav** — drawer is the only persistent nav surface.
- **No app bar elevation / shadows** — flat document feel everywhere. Section boundaries come from borders + spacing, not shadows.
- **Phone-first**. No tablet layout yet. Min Android 8 (SDK 26).

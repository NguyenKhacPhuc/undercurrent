# Undercurrent — Design Spec

Brief for a designer redesigning the app. Captures the current visual language, the constraints, and the open problems.

---

## Brand position

A **private, calm, document-feel AI assistant**. The reading reference is a well-set paperback or a literary magazine — not a chat product or a productivity tool. Words deserve generous space. Serif body type. Minimal chrome. Color used sparingly.

Tone in copy: first-person, warm, plain English. "I'll need an API key" — never "Please enter your API key to continue."

If you're designing from scratch, the easiest mistake to avoid is **looking like every other chat app**: bubble-heavy, neon accents, animated avatars, dense toolbars. Undercurrent intentionally doesn't have those.

---

## Current visual system

### Palettes (4 shipping)
Each palette has a Light + Dark variant. User picks palette and mode independently in Settings → Appearance.

| Palette | Light bg | Light ink | Accent (L/D) | Vibe |
|---|---|---|---|---|
| **Vellum** (default) | `#F5EFE0` sepia | `#3A2E1F` deep brown | `#3A2E1F` / `#D9CFB8` (no chromatic accent — accent IS a darker ink tone) | Pure writing surface. The default. No color, just paper. |
| **Warm Dark Amber** | `#FAF7F2` cream | `#2C2825` warm black | `#B47A1F` / `#E8A547` amber | Calm, writerly. The original "comfortable evening reading" feel. |
| **Sage & Ochre** | `#F4F2EB` cool cream | `#1F2421` near-black | `#6B8E7F` / `#8FB39F` sage | Quieter, more distinct. A muted natural green. |
| **Newsprint** | `#F8F5EF` paper | `#0A0A0A` true black | `#B91C1C` / `#EF4444` editorial red | Highest contrast. The "Sunday paper" feel. |

**Token names** (used in code, useful for design system parity):
- `background` — the page itself
- `surface` — cards / inline containers, ~1 step lighter than bg
- `surfaceMuted` — secondary fills (token chips, callouts), 1 step darker than surface
- `ink` — primary text
- `inkMuted` — body secondary text (~60% of ink contrast)
- `inkSubtle` — captions, placeholder text (~35%)
- `divider` — 1px lines + card borders
- `accent` — single accent color per palette, used for: selected state, primary CTA, accent bars in callouts, links, streaming cursor
- `onAccent` — text/icon color when sitting on the accent fill
- `codeBg` / `codeInk` / `codeBorder` — code block treatment
- `error` — destructive states

### Typography
Three families. Bundled fonts are TBD (currently using system serif / sans / mono — Noto / Roboto / Roboto Mono on most devices). Open to a designer specifying actual font files.

| Token | Family | Size | Weight | Usage |
|---|---|---|---|---|
| `serifBodyLarge` | Serif | 18sp / 28 line | Normal | Onboarding body, long-form |
| `serifBody` | Serif | 16sp / 24 line | Normal | Assistant messages, persona names, conversation rows |
| `sansHeader` | Sans | 15sp / 20 line | SemiBold (-0.1 tracking) | Screen titles, provider names, button labels |
| `sansLabel` | Sans | 11sp / 14 line | Medium (0.5 tracking) | UPPERCASE role tags, section labels |
| `sansSmall` | Sans | 12sp / 16 line | Normal | Captions, taglines, helper text |
| `mono` | Monospace | 13sp / 19 line | Normal | Code blocks, API keys, model ids |

Headings inside markdown messages scale `serifBody` by 1.3 / 1.5 / 1.7 (H3 / H2 / H1).

### Shapes
| Token | Radius |
|---|---|
| `xsmall` | 4dp |
| `small` | 8dp |
| `medium` | 12dp |
| `large` | 16dp |
| `xlarge` | 24dp |

Most cards + buttons use `medium`. Pills + chips use `xsmall`.

### Spacing
8-point grid, mostly. Typical paddings: 14 / 16 / 20 / 24 dp. Section spacing 12 / 16 dp.

### Elevation
**None.** No shadows, no elevation tokens. Hierarchy comes from borders + surface color steps. This is intentional — flat document feel.

---

## Component patterns currently in use

### ScreenScaffold
Standard top bar (`Header`: back arrow + title + optional trailing action) over a content slot. Every secondary screen uses this. Title is sansHeader, no centered iOS-style title.

### Cards
Border 1dp `divider`, radius `medium`, fill `surface`, padding 14–16dp horizontal × 14dp vertical. When selected: border 2dp `accent`. No shadow, ever.

### Section labels
UPPERCASE `sansLabel` in `inkSubtle`. Used to group rows in lists (e.g. "Today" / "Yesterday" in Conversations, "Built-in" / "Custom" in Personas, "Theme" / "Mode" in Appearance).

### Divider rows
1dp `divider` line between list items. Edge-to-edge (no inset).

### TipBox (callout)
3dp accent-colored left bar + `surfaceMuted` fill, padding 14×12dp. Optional UPPERCASE title + `sansSmall` body. Used to explain unfamiliar concepts inline (currently: "About tiers" on Provider Settings).

### ScaffoldTextAction
Inline text button. No background. Color is `accent` (or `error` when `isDestructive=true`). Used for "+ New", "Save key", "Clear", "Delete" — all the inline actions on a card or in a header.

### Token chip
Small rounded rect with `xsmall` shape, `surfaceMuted` background, 6×2dp padding, `sansLabel` text. Used for status pills (RUNNING / OK / FAIL on trace rows), tool-status chips in chat, persona scope chips on memory rows.

### Primary CTA
Filled rect, `accent` background, `onAccent` text in `sansHeader`, `medium` shape, 22×12dp padding. Used sparingly — onboarding "Let's go →", any single primary action on a screen.

### Inline text field (chat input, key paste, custom persona dialog)
Border 1dp `divider`, radius `small`, fill `surface` or `background`, padding 12×10dp. Cursor color is `accent`. No floating label, no focus ring — placeholder text in `inkSubtle` replaces a label.

### Segmented control (Material 3)
For mutually-exclusive small-set choices: Auto/Light/Dark, Auto/Cheap/Standard/Heavy. Full width.

### Drawer (Material 3 ModalNavigationDrawer)
Slides from left. Top: app name + recent conversations grouped by recency. Bottom: nav links with → on each row.

### Markdown (custom, not a library)
Assistant messages render as styled text:
- Paragraphs in `serifBody`
- Headings scale up (H3 1.3× → H1 1.7×) in `serifBody` SemiBold
- Bullet/numbered lists with hanging indent
- Inline `code` in `mono` on `codeBg` background
- Fenced ` ``` ` blocks in `mono` on `codeBg`, padding 12dp, scrollable horizontally
- **bold**, *italic*, [links](url) — links open in Chrome Custom Tabs, accent-underlined

---

## Pain points / open problems for the designer

These are real things the current visual / interaction design hasn't solved well. Useful starting points for a redesign brief.

### 1. Drawer is unbalanced
Recent conversations dominate the drawer; the nav links sit underneath as small text rows. On a long conversations list, nav is below the fold. Either compress the conversation list (cap at 5? collapse by default?) or restructure the drawer entirely.

### 2. Chat input row is getting busy
Currently in the bottom row: [tier chip] [text field] [mic] [send]. With voice + tier picker, the field width is squeezed. As we add (file attach? camera? slash menu trigger?), this row will break.

### 3. "Models customization" lives 3 taps deep
Settings → Provider → expand a provider card → expand "Models". Defensible (most users won't touch it), but the discoverability of model overrides is poor. Maybe a dedicated screen.

### 4. Tool calls in the message stream
Currently tool calls render as inline single-line status chips ("running: notes_search" → "✓ notes_search done"). They mix into the message flow. Question: should they fold into a collapsible group? Sit in a sidebar? Render below the assistant reply as a "what I did" footer?

### 5. No visual identity beyond "document feel"
There's no logo, no app icon design beyond the launcher default, no illustration style, no empty-state characters. The brand is a posture (serif type, sepia surface, restraint) but not a visual identity. Open canvas.

### 6. Onboarding is wordy
Three text-heavy pages with no illustration. Sets the literary tone, but might lose users who scan rather than read. Could one of them carry a visual element (a hand-drawn diagram of "where data lives," for instance)?

### 7. Streaming feedback is minimal
A blinking cursor under the in-progress reply is the only signal that something's happening. No skeleton, no progress, no thinking indicator. Some users have asked "is it stuck?" — especially when the model spends 5–10s on a tool call before any text streams.

### 8. Tier picker labels are abstract
"Cheap / Standard / Heavy / Auto" — meaningful to engineers, opaque to users. We added a TipBox to explain, but the labels themselves could be more direct (Fast / Balanced / Smart? Quick / Default / Deep?).

### 9. Personas screen lacks personality
Five built-in personas are listed as plain text rows. The personas themselves have voice + tone, but the UI rendering them doesn't. Could each persona have a visual badge / micro-illustration / type sample?

### 10. No empty states with character
Conversations / Memories / Traces all show centered-text empty states. Functional but flat. Real opportunity for the brand to show up.

---

## Technical constraints (what the design must respect)

- **Phone-first, portrait-primary**. No landscape layouts. Tablet is a future consideration.
- **Android 8+** (API 26). All effects available. No Material You dynamic color — we have our own palette system.
- **Material 3 base** (M3 component library is allowed where useful — Segmented buttons, ModalNavigationDrawer, AlertDialog. But we're not married to M3 visuals — we override aggressively).
- **No shadows or elevation tokens.** Hierarchy via color + spacing only. Designs that lean on M3 elevation will not translate.
- **Compose-renderable.** No SVG complex paths that can't be `androidx.compose.ui.graphics.Path` — keep custom illustrations simple, or plan to bundle vectors.
- **Theme-switchable.** Anything you design must work in all 4 palettes × light/dark = 8 combinations. Color tokens above are the entire surface — design against tokens, not hex codes.
- **Dark mode is first-class.** Vellum dark + Warm dark are the daily-drive themes. Designing only for light = half-done.
- **No bundled fonts yet.** If you spec a custom font, we'll bundle it. Default fallback must look acceptable.

---

## Deliverable suggestions for the redesign

Useful artifacts to ask for (in priority order):

1. **Brand pass** — logo, app icon, illustration style (even if minimal). The thing that's currently missing entirely.
2. **High-fi mockups of the 5 most-visited screens**: Chat (with one streaming + one tool call), Conversations list, Personas, Provider settings, Onboarding (page 1 + page 3).
3. **Component sheet** — re-spec the cards / buttons / chips / inputs / dividers using the token system above. Keep token names, change values.
4. **Empty + error + loading states** for every screen above.
5. **Palette refresh** (optional) — propose tweaks to any of the 4 palettes, or propose a 5th. If proposing changes, supply both light + dark hex for every token.
6. **One signature interaction** — pick a moment (onboarding-finish? first reply streaming in? regenerate tap?) and design a small expressive flourish. The app is restrained on purpose; one or two moments of expression go a long way.

---

## What the designer should NOT do

- Don't add gradients except possibly the streaming-cursor / waveform animations.
- Don't change to a bubble-style chat (each message in a colored capsule). The serif-on-page treatment is the whole point.
- Don't add neon / saturated accent colors. The 4 accents (sepia, amber, sage, editorial red) are deliberately muted.
- Don't redesign for a side-by-side / multi-pane layout. It's a single-pane phone app and will stay so.
- Don't propose a bottom nav or tab bar. The drawer is the nav.

---

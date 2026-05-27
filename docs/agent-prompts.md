# Undercurrent — Agent UI Test Prompts

A categorized set of user messages designed to exercise every UI component
in the Undercurrent palette. Copy a prompt into chat and watch what the
agent renders.

> **Setup**: install fresh debug build (`./gradlew :app:installDebug`)
> after the component changes so the new system prompt + registry pick
> up. Force-stop first so the runtime doesn't reuse the old tool catalog.

## Component coverage matrix

| Category | Components | Section |
|---|---|---|
| Editorial | Heading, Text, Quote, Markdown, PhotoFrame, Spotlight | [#1](#1-editorial--prose) |
| Dashboard | Stat, Sparkline, ProgressRing, LineChart, BarChart, Donut, Grid | [#2](#2-dashboard--data-viz) |
| Lists | ListRow, KeyValue, Checklist, Timeline, Steps | [#3](#3-lists--rows) |
| Forms | Field, Toggle, Stepper, SegmentedToggle, Rating, MoodScale, DateStrip, Composer, Choice | [#4](#4-forms--inputs) |
| Code | CodeBlock, Diff | [#5](#5-code--diff) |
| Feedback | Banner, Empty, Skeleton, Note | [#6](#6-feedback--state) |
| Navigation | Tabs, Reveal, BottomSheet (substrate) | [#7](#7-tabs--reveal) |
| Plan mode | exit_plan_mode flow | [#8](#8-plan-mode-flow) |
| Stress | Mixed full-screen layouts | [#9](#9-mixed-stress-prompts) |

---

## 1. Editorial / prose

### 1.1 Pull-quote with attribution
```
Show me a pull quote: "We shape our buildings; thereafter they shape us."
— Winston Churchill. Center it on the screen.
```
Expected: `Quote` component with leading mark, italic serif body, "— Winston Churchill" attribution.

### 1.2 Heading with kicker + body paragraph
```
Lay out the opening of an essay titled "The Quiet Architecture" with the
kicker "essays" above it, followed by a short serif paragraph using
markdown: **architecture** is the *thoughtful* making of space.
```
Expected: `Heading` (kicker + display title) followed by `Markdown` with **bold** and *italic* spans.

### 1.3 Hero photo with caption
```
Show a photo frame with the image
https://images.unsplash.com/photo-1490604001847-b712b0c2f967?w=1200,
caption "Morning fog over the Marin Headlands", credit "Photo by Aaron
Burden".
```
Expected: `PhotoFrame` with image, italic caption, small credit.

### 1.4 Spotlight card
```
Render a Spotlight card for a featured note:
- kicker "this week"
- title "What I learned about systems thinking"
- body "Three patterns kept showing up — feedback loops, stocks vs flows, and delays."
- image https://images.unsplash.com/photo-1532012197267-da84d127e765?w=800
- CTA "Read note" → action open_essay
```

---

## 2. Dashboard / data viz

### 2.1 Daily stats grid (2x2)
```
Give me a quick dashboard with 4 stat cards in a 2-column grid:
- Notes today: 12 (+3 trend up)
- Focus minutes: 84 (-6 trend down)
- Tasks done: 7 of 9 (flat)
- Mood avg: 4.2 (+0.4 trend up)
Each stat lives in a soft Sheet.
```
Expected: `Grid` (columns=2) of `Sheet` (soft) each with a `Stat`. Trend coloring kicks in.

### 2.2 Line chart of weekly mood
```
Plot my mood scores for the last 7 days as a line chart:
3, 4, 4, 3, 5, 4, 5 with day labels M T W T F S S. Show the latest value
and an area fill.
```
Expected: `LineChart` with dashed grid, accent line, translucent area, "5" chip below.

### 2.3 Bar chart with rotating palette
```
Show a bar chart of hours spent per category this week:
Work 22, Reading 6, Exercise 4, Sleep 56, Cooking 5. Use the rotating
palette and label each bar.
```
Expected: `BarChart` with 5 bars, each in a different accent-derived hue.

### 2.4 Donut breakdown
```
Show a donut chart of my note tag distribution: ideas 8, journal 5,
reference 3, todos 4. Show legend, center the total under "notes".
```
Expected: `Donut` with 4 segments, center text "20", subtext "notes", legend below.

### 2.5 Sparkline inside a stat
```
Render a Sheet containing a stat for "weight" 168.4 (-1.2 down) with a
small sparkline of the last 14 readings: 170, 170, 169, 169, 168, 168,
167, 168, 168, 167, 167, 168, 168, 168.4.
```
Expected: `Sheet` → `Stack` → `Stat` + `Sparkline` (sm, trend tone).

### 2.6 Progress ring
```
Show me a ProgressRing at 72%, label "72%", caption "of weekly goal",
size large.
```

---

## 3. Lists / rows

### 3.1 Tappable list of mini-apps
```
Render a bordered Sheet containing a list of these tappable rows, each
with an icon and chevron:
- Journal (notes icon) → open_journal
- Habits (bookmark icon) → open_habits
- Reading log (notes icon) → open_reading
- Workouts (bolt icon) → open_workouts
```
Expected: `Sheet` (bordered) → `Stack` of `ListRow`s, each with icon + chevron + onTap.

### 3.2 Definition list (KeyValue)
```
Show me the metadata for a note as a KeyValue list with emphasized keys:
- Created: Jul 14, 2025
- Modified: 2 hours ago
- Words: 1,247
- Tags: ideas, systems
- Source: meeting w/ Alex
```

### 3.3 Checklist
```
Render a checklist of today's tasks:
- t1 "Review the design doc"
- t2 "Reply to Maria's email" (checked)
- t3 "Plan tomorrow's standup"
- t4 "Read chapter 4" (checked)
- t5 "Walk the dog"
```
Expected: `Checklist` — tapping items toggles strikethrough.

### 3.4 Timeline of plan steps
```
Show the steps for releasing version 1.1 as a Timeline:
1. "Bump version" (9:14am, done)
2. "Run the test suite" (9:20am, done)
3. "Cut release branch" (active, body "waiting on CI")
4. "Tag and publish" (pending)
5. "Announce in #releases" (pending)
```

### 3.5 Steps progress (1 of 4)
```
Show a Steps progress with current = 1: "Account", "Profile", "Preferences", "Review".
```

---

## 4. Forms / inputs

### 4.1 Sign-up form
```
Build me a sign-up form inside a Section titled "Create your account":
- Field "email" label "Email", hint "We'll never spam"
- Field "password" label "Password" hint "8+ characters"
- Toggle "newsletter" title "Send me weekly updates" description "Once a week, never more"
- Button onTap=create_account label "Create account"
```

### 4.2 Mood + rating logger
```
Help me log how today went:
- MoodScale id "mood_today" initial -1
- Rating id "energy" max 5
- Composer id "what_happened" placeholder "What stood out?" onSubmit save_journal label "Save entry"
```
Expected: emoji row + 5 stars + multi-line note with send button.

### 4.3 Segmented view toggle
```
Show a SegmentedToggle id "range" with options ["Day","Week","Month","Year"]
initial 1, then a Banner saying "showing this week" tone info.
```

### 4.4 Stepper + DateStrip
```
Render a Stepper id "servings" label "Servings" initial 2 min 1 max 12,
and below it a DateStrip id "when" with days Mon=8, Tue=9, Wed=10
(selected), Thu=11, Fri=12 marked, Sat=13, Sun=14.
```

### 4.5 Choice group
```
Show three Choice rows in a Stack — "Quiet (do not disturb)" icon notes,
"Focus" icon bolt selected, "Open" icon inbox. Each has an action like
mode_quiet / mode_focus / mode_open.
```

---

## 5. Code / diff

### 5.1 Code sample with language label
```
Show this snippet in a CodeBlock with language "kotlin":

fun greet(name: String) {
    println("Hello, $name!")
}
```

### 5.2 Diff
```
Show a Diff with title "config.yaml":
 name: app
-version: 1.0
-debug: true
+version: 1.1
+debug: false
 retries: 3
```
Expected: red/green tinted lines with + / − markers.

---

## 6. Feedback / state

### 6.1 Banner with CTA
```
Show a warning Banner: title "Storage almost full", text "You've used
94% of your 1 GB quota.", action label "Free up space" key open_storage.
```

### 6.2 Empty state
```
Show an Empty placeholder: icon inbox, title "No notes yet", body
"Capture a thought to get started.", CTA "New note" → new_note.
```

### 6.3 Skeleton loader
```
Render 5 Skeleton lines while we wait. After it I want you to also show
me a Note tone info saying "Loading…".
```

### 6.4 Note callouts (all four tones)
```
Show four stacked Notes — one each of info, success, warning, error,
each with a different message and the default per-tone icon.
```

---

## 7. Tabs / reveal

### 7.1 Tabs with three panels
```
Build a Tabs view with tabs ["Today", "Week", "Month"]. Each tab gets
one child:
- Today: a Stat 12 "Notes" + sparkline of 7 points 2,3,3,4,5,4,6
- Week: a BarChart of values 12,15,9,18,14,16,11 with day labels
- Month: a Heading kicker "since Jul 1" text "342 notes"
```

### 7.2 Reveal (collapsible)
```
Render a Reveal with title "Advanced settings", subtitle "Power users
only", initialOpen false. Body is a Stack with two Toggles — "Show
debug overlay" and "Verbose logs".
```

---

## 8. Plan-mode flow

> Plan mode = the agent suggests a structured plan via `exit_plan_mode`,
> waits for approve / refine / cancel, then executes only after approval.
> **Setup**: flip the session into plan mode before sending these prompts.
> Until the host UI exposes a toggle, do it from MainActivity / a test
> hook: `planSession.enter()` before sending.

### 8.1 Build a habit tracker (happy path)
```
Build me a small habit tracker mini-app. I want to add habits, mark
them done each day, and see a streak count.
```
Expected: agent reads / asks if needed, then calls `exit_plan_mode` with
a multi-step plan. The plan-review sheet (default `askUser` fallback)
shows Approve / Refine / Cancel. On Approve, the agent gets "Plan
approved" and starts executing (calls write tools).

### 8.2 Refine loop
```
I want a journaling mini-app with prompts and a mood scale on each
entry.
```
At the plan prompt, tap **Refine** and type *"add a weekly review
screen too"*. Expect the agent to call `exit_plan_mode` again with a
revised plan that includes the weekly review step.

### 8.3 Cancel
```
Make me a kanban board for project tracking.
```
At the plan prompt, tap **Cancel**. Expect the agent to acknowledge and
stop without running write tools.

### 8.4 Confirm gating works
While in plan mode, ask the agent to:
```
Save a note that says "test from plan mode".
```
Expected: agent refuses because `data_update` is Write-class. It should
either propose a plan or ask the user to leave plan mode first.

---

## 9. Mixed / stress prompts

### 9.1 Full morning dashboard
```
Render my morning dashboard:
- Heading kicker "Tuesday" text "Good morning, Phuc." level 1
- A 2-column Grid of stat sheets (notes today, focus mins, tasks done,
  mood) — invent the numbers
- A Section titled "This week" with a LineChart of focus minutes
- A Timeline of today's plan (3-5 entries)
- A Banner suggesting one focus block at 2pm with CTA "Block calendar"
```

### 9.2 Reading log entry
```
Help me log a book I finished — "The Beginning of Infinity" by David
Deutsch. Build a screen with:
- a Spotlight at top with kicker "just finished", title, body (one
  sentence about why I liked it)
- a Rating id rating max 5
- a Composer id review placeholder "What stuck with you?" submit
  save_review
- a KeyValue list of metadata (pages, started, finished, format)
```

### 9.3 Settings screen
```
Build a settings screen:
- Section "Account" with two ListRows (Profile + chevron, Subscription
  trailing "Pro")
- Section "Appearance" with a SegmentedToggle for theme (Light, Dark,
  Auto) and a Toggle "Reduce motion"
- Section "Danger zone" with a Button variant ghost label "Sign out"
  onTap sign_out and a Button variant secondary label "Delete account"
  onTap delete_account
```

### 9.4 Pure editorial article layout
```
Lay out a magazine-style article:
- Heading level 1 with kicker "field notes"
- A Spotlight (cover image) — use any Unsplash photo
- 2-3 paragraphs of Markdown body with **bold** and *italic*
- A Quote midway through
- A PhotoFrame with caption and credit
- A Hairline tone accent at the end
```

### 9.5 Workout logger
```
Help me log today's workout:
- Heading level 2 "Strength, Tuesday"
- A Stack of ListRows for exercises (Squat, Bench, Row) each with
  trailing text like "5×5 @ 185"
- A Stepper id rpe label "RPE" initial 7 min 1 max 10
- A MoodScale id energy
- A Composer id notes placeholder "How did it feel?"
- Submit Button onTap save_workout
```

---

## What to watch for

- **Theme adherence** — palette swaps in Settings should re-color everything.
  Try toggling to a darker palette mid-session.
- **Tone of voice in descriptions** — the agent should naturally reach
  for the right component (e.g. "list of …" should produce ListRow not
  raw Text bullets).
- **Plan-mode gating** — verify writes are actually blocked while in
  plan mode, and that approval flips the mode back automatically.
- **Tap targets** — chevron / TapCard / ListRow with onTap should fire
  the action; check trace bubbles in the chat for the synthesized
  `[UI event] User tapped …` user message.
- **No crashes on bad input** — try asking for a `LineChart` with one
  point, or an unknown icon name. Should gracefully degrade.

## 10. Creator flows (Personas + Mini Apps)

> Guided QnA wizards where the agent generates each question dynamically.
> Two entry paths:
>
> - **Settings (locked wizard)** — Drawer → Personas → "+ New" on the
>   Voices or Roles section, OR Drawer → Mini apps → "New mini-app".
>   Lands on the dedicated Creator screen (no chat input bar — user
>   answers ONLY via the agent's rendered widgets, must go through
>   every step).
> - **Chat (free conversation)** — type a request in the regular chat;
>   the agent gathers details conversationally and calls
>   `create_persona` / `create_mini_app` when ready.

### 10.1 Settings → New voice persona (happy path)

1. Drawer → Personas → tap "+ New" in the **Voices** section.
2. Creator screen appears with "Create voice" header. After ~1s the
   agent renders the first question — a `Field` for the persona name.
3. Fill in `"Newsletter writer"`, tap Next.
4. Agent renders next question (e.g. `Choice` between Concise / Lyrical /
   Technical / Playful tones). Pick one.
5. Repeat for ~4-6 questions covering: style, tone, constraints,
   one-line tagline.
6. Agent calls `create_persona` → screen dismisses → lands on Personas
   list with the new entry already **active** (highlighted in Voices).
7. Pop back to chat and send any message — verify the reply reflects
   the new voice.

### 10.2 Settings → New role persona

Same as 10.1 but tap "+ New" in the **Roles** section. The agent should
ask about the role's domain (e.g. "Pediatrician", "Tax attorney") and
generate a system prompt with appropriate disclaimers (medical/legal
roles must include "not a substitute for a licensed clinician/attorney").

### 10.3 Settings → New mini-app (happy path)

1. Drawer → Mini apps → tap the "+ New mini-app" row at the top of the
   list (or the CTA button in the empty state).
2. Agent asks what the mini-app should do. Try
   `"Daily mood check-in — give me a quick mood + energy + one-line note"`.
3. Agent walks through name, emoji, trigger prompt — should propose a
   trigger prompt that re-runs the same kind of UI.
4. Approve, screen dismisses, land on Mini Apps list with the new
   entry. Tap it to invoke for the first time → cached UI is populated.

### 10.4 Settings → Cancel mid-flow

1. Start any creator flow.
2. Answer one or two questions.
3. Tap the **X** (top-left). Screen dismisses back to the originating
   list. Conversation is discarded; nothing was persisted.

### 10.5 Chat → "Make me a persona for X"

In a normal chat (Drawer → start a new chat):

```
Make me a custom voice that writes like Anne Lamott — funny, self-deprecating,
faith-curious without being preachy.
```

Expected: agent asks a few clarifying questions in the chat (or just
calls `create_persona` directly if it has enough). Persona lands in the
Voices section. Switch back to chat → reply reflects the new voice.

### 10.6 Chat → "Save this as a mini-app"

After a successful turn that rendered useful UI:

```
That worked well — save this as a mini-app called "Morning notes" with
a 🌅 emoji. The trigger should be "give me my morning dashboard".
```

The agent should call `create_mini_app` directly (no QnA needed —
all three fields were specified).

### 10.7 Creator-mode gating

While inside a Creator screen, the user can only interact through the
agent's rendered widgets. Try to verify:

- No regular chat input bar visible.
- Cancel button works.
- Tapping a Field, typing, then tapping the agent's "Next" Button
  advances to the next question.
- After ~4-6 answers, the screen auto-dismisses with the new entry
  created.

### 10.8 Persona system prompt quality

After 10.1 or 10.5 completes, drill into Personas → long-press the new
entry → check the system-prompt text. It should be:

- 3-6 sentences.
- Action-leading ("Read what the user sends…", not "This persona is…").
- Concrete (mentions specific behaviors, not vague adjectives).
- No leading "You are a…" boilerplate (the runtime adds framing).

If the prompt is weak, tighten the creator preamble in
[`AppModule.kt:creatorPreambleFor`](app/src/main/kotlin/dev/weft/undercurrent/di/AppModule.kt).

## 11. Second-wave components

> 17 additions that round out the palette: forms (sliders, ranges,
> multi-select, color), calendar/time, advanced viz (heatmap, gauge,
> comparison), productivity (tasks, kanban), navigation (search, chips,
> pagination), meta (status, chat, link preview).

### 11.1 Sliders (single + range)
```
Help me set a daily reading goal. Show a Slider id "minutes" label
"Minutes per day" min 0 max 120 initial 30 steps 11 unit "mins" format
"int". Below it a RangeSlider id "window" label "Quiet hours" min 0 max
24 initialStart 22 initialEnd 7 format "int" unit "h".
```

### 11.2 MultiChoice + ColorPicker
```
Pick my preferences:
- MultiChoice id "diet" title "Dietary preferences" with options:
  vegetarian, vegan, gluten-free, dairy-free (default gluten-free
  checked).
- ColorPicker id "accent" label "Choose your accent" with default
  palette, initial "#0EA5E9".
- A Button label "Save preferences" onTap save_prefs.
```

### 11.3 Calendar (month grid)
```
Show me a Calendar id "schedule" for May 2026, selected 2026-05-26,
with these days marked (had activity): 2026-05-01, 2026-05-03,
2026-05-10, 2026-05-15, 2026-05-22, 2026-05-26.
```

### 11.4 Countdown to a target
```
Countdown to 2026-06-15 size lg with label "until vacation". Then
another Countdown to "2026-05-25" (a past date) showing 'overdue by'
state.
```

### 11.5 Heatmap (activity grid)
```
Show a Heatmap of my last 8 weeks of activity, label "Recent weeks",
rows 7. Use these values (column-major, 7 per week):
[0,0,1,2,0,1,3, 2,3,4,2,1,2,3, 1,2,3,4,5,3,2, 3,4,2,1,0,1,2,
2,3,4,5,4,3,2, 3,4,5,4,3,2,1, 4,3,2,3,4,5,4, 2,3,4,3,2,1,2]
```

### 11.6 Gauge
```
Show three Gauges in a Grid columns=3:
- Battery 0.84 caption "battery" tone good size sm
- CPU 0.62 caption "cpu" tone warn size sm
- Storage 0.91 caption "storage" tone bad size sm
```

### 11.7 Comparison table
```
Compare two coffee makers:
- leftLabel "Aeropress" rightLabel "V60"
- Rows:
  - Price: $35 vs $25 (right wins)
  - Cleanup: Easy vs Medium (left wins)
  - Travel-friendly: Yes vs No (left wins)
  - Brew time: 1.5 min vs 3 min (left wins)
  - Cup count: 1 vs 1-3 (right wins)
```

### 11.8 Task list (TaskItems in a Sheet)
```
Show today's task list in a bordered Sheet:
- TaskItem id "t1" title "Send the design doc" due "Today, 5pm"
  priority "high" tag "Work"
- TaskItem id "t2" title "Reply to Maria" done true priority "med"
  tag "Work"
- TaskItem id "t3" title "Read chapter 4" due "Tonight"
  priority "low" tag "Personal"
- TaskItem id "t4" title "Walk the dog" priority "low"
```

### 11.9 Kanban board (3 columns)
```
Render a 3-column kanban board for the redesign project. Use Inline
with horizontal scroll and three KanbanColumns:
- "To do" tone accent — 2-3 tasks
- "Doing" tone warning — 1 task
- "Done" tone success — 3 tasks (all done=true)
Each task is a TaskItem with realistic placeholder titles + due dates.
```

### 11.10 SearchBar + FilterChips + Pagination
```
Above a (placeholder) list, render:
- a SearchBar id "q" placeholder "Search notes…"
- a FilterChips id "tags" multi true with chips:
  ideas (selected), todo, reference, journal, drafts
- below the list (just imagine for now), a Pagination id "page" page 3
  pages 12.
```

### 11.11 Status indicators
```
Show four Status rows in a Stack:
- "Online" tone online pulse true detail "just now"
- "Away" tone away detail "5 min ago"
- "Busy — in a meeting" tone busy
- "Offline" tone offline detail "yesterday"
```

### 11.12 ChatBubble transcript
```
Show a short conversation as ChatBubbles inside a Sheet:
- assistant "Hey — what should we work on first?" authorName "Claude"
  label "now"
- user "Let's start with the data model" from user label "you"
- assistant "Sounds good. I'll sketch three options." authorName
  "Claude" label "a moment ago"
- system "Conversation auto-saved" from system
```

### 11.13 LinkPreview cards
```
Show three LinkPreviews stacked:
- nytimes.com — title "Climate, Coffee, and the Future", description
  "How brewing habits are changing in the age of climate uncertainty.",
  imageUrl (any unsplash coffee photo URL), onTap open_link
- github.com — title "weft / android-harness", description "An Android
  SDK for LLM-orchestrated apps.", onTap open_link
- youtube.com — title "Lecture: Systems thinking 101"
```

### 11.14 Mixed second-wave dashboard
```
Build me a small "today" dashboard that uses the new components:
- Heading kicker "Tuesday" text "Good morning" level 1
- A Status row "Streak active" tone online pulse true detail "Day 12"
- Inline two Gauges (battery + focus) plus a Countdown to a deadline
- A Heatmap of the last 8 weeks of activity
- A Comparison between "Yesterday" and "Today" on words written / focus
  minutes / breaks taken
- Three TaskItems for today
```

### 11.15 Plan-mode interaction with second wave

Try entering plan mode (§8) and asking:
```
Build me a habit tracker mini-app with a Calendar + Heatmap view, a
Stepper for daily intake, and a Gauge for the weekly goal.
```
Expected: agent's plan covers structure → on approval, executes by
rendering the planned screen using the new components.

## 12. Third-wave components

> 15 more — media playback affordances, specialized form inputs,
> commerce primitives, overlay surfaces, and small signal indicators.

### 12.1 AudioPlayer
```
Show an AudioPlayer id "track1" title "Morning meditation" artist
"Calm" durationSec 600 positionSec 142 playing true.
```

### 12.2 VideoFrame
```
Show a VideoFrame thumbnailUrl "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?w=1200"
title "Espresso pulled at home" durationLabel "3:42" onPlay play_video.
```

### 12.3 ImageGallery
```
Show an ImageGallery id "trip" with 5 images (use any unsplash URLs):
beach, mountain, market, sunset, street. Captions: "Beach day",
"Mountain morning", "Market", "Sunset", "Side street".
```

### 12.4 OtpInput
```
I'm verifying my email. Show an OtpInput id "code" label "Enter the
6-digit code" length 6 hint "Sent to phuc@example.com".
```

### 12.5 PhoneField + CurrencyField in a form
```
Help me log an international payment:
- PhoneField id "to" label "Recipient phone" dialCode "+84" hint "Enter without leading 0"
- CurrencyField id "amount" currency "VND" label "Amount" hint "Daily limit 50M VND"
- Button label "Send" onTap send_payment fullWidth true.
```

### 12.6 Receipt
```
Show a receipt for this morning's order:
- title "Blue Bottle Coffee", subtitle "Today, 8:14am"
- items: Latte (12oz × 1, $5.25), Croissant ($4.50), Cold brew ($4.75)
- summary: Subtotal $14.50, Tax $1.30, Tip $2.50
- total: $18.30.
```

### 12.7 ContactCard
```
Show a ContactCard for Maria Chen, role "Design Lead", handle "@maria"
with three actions: message (icon "share", onTap message_maria),
open profile (icon "open", onTap open_profile), and call (icon "bolt",
onTap call_maria).
```

### 12.8 Reaction
```
Below a (placeholder) post, show a Reaction bar id "post42" with:
❤️ count 12 reacted true, 👀 count 4, 💡 count 7, 🔥 count 2. Allow add.
```

### 12.9 Dialog
```
Show a Dialog with icon "warning", title "Delete this conversation?",
body "This can't be undone — 47 messages will be removed.", actions:
Cancel (variant ghost, onTap cancel) and Delete (variant primary,
destructive true, onTap confirm_delete).
```

### 12.10 Toast
```
Show four Toasts stacked, one each tone:
- info: "Synced 12 minutes ago"
- success: "Note saved"
- warning: "Connection unstable"
- error: "Send failed — tap to retry".
```

### 12.11 Trend + Stat pairing
```
Show three Stats in a Grid columns=3, each followed by a Trend below:
- Notes today: 24, Trend "+8" label "vs yesterday" direction up
- Focus mins: 92, Trend "-12" label "vs avg" direction down
- Streak: 12, Trend "0" label "no change" direction flat
```

### 12.12 DotPaginator
```
Render: a Stack with five Headings (level 3) showing pages, then a
DotPaginator total 5 current 2. Above it, a Heading "Onboarding step 3
of 5".
```

### 12.13 WeatherCard
```
Show me a weather card for Brooklyn — 72°, partly cloudy, kind cloudy,
H 78° / L 64°. Then a second card for "Hanoi, VN" — 34°C, kind sunny,
condition "Sunny".
```

### 12.14 TipCard
```
Show three TipCards stacked:
- icon "lightbulb", title "Try plan mode", text "Long-press the + button
  to enter plan mode — Claude will draft a plan before doing anything
  risky.", onDismiss dismiss_tip_plan
- icon "bolt", title "Save successful turns", text "When a rendered UI
  works the way you want, tap Save in the top right to keep it one tap
  away.", onDismiss dismiss_tip_save
- icon "favorite_outline", title "Personas", text "Voices change how
  Claude writes. Roles change what they know.", onDismiss dismiss_tip_personas
```

### 12.15 Mixed third-wave: order confirmation
```
Build me an order confirmation screen:
- Heading kicker "Order placed" text "You're all set" level 2
- A Receipt with realistic line items and total
- A ContactCard for the courier (made-up name, "Courier" role, message
  action)
- A Toast tone success "ETA 12 min"
- A VideoFrame placeholder showing a (fake) tracking video.
```

## 13. Fourth-wave components

> 6 more — hero card, event/location/status indicators, pricing tier
> card, and a generic sectioned StructuredList that subsumes recipe /
> packing-list / guide layouts.

### 13.1 Hero
```
Show a Hero with imageUrl
"https://images.unsplash.com/photo-1518391846015-55a9cc003b25?w=1200",
kicker "this week", title "A walking tour of Brooklyn",
subtitle "From DUMBO to Prospect Heights, with stops along the way.",
aspect "16/9", ctaLabel "Read the guide", ctaKey read_guide.
```

### 13.2 EventCard list
```
Show my next three meetings as EventCards in a Stack:
- MAY 30 — "Design review", 2:00-3:30pm, "Conference room B", 6 people
- MAY 31 — "1:1 with Maria", 10am, "Zoom"
- JUN 02 — "Quarterly planning", 9am-12pm, "Main room", 14 people
Each tappable (onTap open_event_X).
```

### 13.3 LocationCard + OpenStatus pairing
```
Show a Sheet with:
- LocationCard "Blue Bottle Coffee" address "85 Dean St, Brooklyn"
  coords "40.6892,-73.9839" distance "0.4 mi" onTap open_directions
- OpenStatus status "closing_soon" detail "Closes at 6pm"
```

### 13.4 PriceCard comparison
```
Show three PriceCards in a Grid columns=3:
- Free: $0/mo, features [Notes (basic), 1 persona, Cloud sync, "- Custom
  components"], CTA "Get started" onTap pick_free
- Pro: $12/mo, FEATURED, tagline "For power users", features
  [Unlimited notes, Custom personas, Priority support, Mini-apps], CTA
  "Start Pro" onTap pick_pro
- Team: $24/mo, features [Everything in Pro, Shared workspaces, SSO,
  Audit log], CTA "Contact sales" onTap pick_team
```

### 13.5 StructuredList — recipe
```
Show me a Carbonara recipe as a StructuredList:
- title "Carbonara", subtitle "20 min · 2 servings"
- Ingredients section (meta "for 2", style "bullet"): 200g spaghetti,
  100g guanciale, 2 egg yolks + 1 whole egg, 60g pecorino finely
  grated, Black pepper (lots)
- Steps section (style "number"): boil + cook pasta, render guanciale,
  whisk eggs + cheese + pepper, drain pasta and toss, off heat add egg
  mix + splash of water and stir until creamy
```

### 13.6 StructuredList — packing list
```
Pack me for a 3-day Berlin trip in October. Use a StructuredList with
sections:
- Clothing (style check): 4 t-shirts, 2 sweaters, raincoat, jeans, etc.
- Tech (style check): charger, adapter, headphones, etc.
- Documents (style check): passport, EU adapter, transit card, etc.
```

### 13.7 Mixed fourth-wave — event detail
```
Build me an event detail screen:
- Hero with imageUrl, kicker "Saturday", title "Outdoor cinema night",
  subtitle "Stuart Park · Doors 7pm", ctaLabel "RSVP" ctaKey rsvp
- Below, an EventCard for the event (MAY 31, "Outdoor cinema night",
  8pm, "Stuart Park", "23 going")
- A LocationCard with address + distance
- An OpenStatus tone "open" detail "Until 11pm"
- A Reaction bar (👀 5, 🎬 12, 🍿 3) for excitement vibes
```

## 14. Fifth wave — primitives

> 5 small but useful primitives: @-mention, person chip, formatted
> money display, weekly hours table, vote affordance.

### 14.1 Mention inline
```
Show a Stack with a Text saying "Assigning to" then an Inline with
three Mentions: @maria onTap open_profile_maria, @alex onTap
open_profile_alex, @sam onTap open_profile_sam.
```

### 14.2 PersonChip attendees row
```
Show the attendees for tomorrow's standup as an Inline of PersonChips:
Maria (ring online), Alex (ring busy), Sam (ring neutral), Jamie (ring
online), Priya (ring neutral). fitContent true on the Inline.
```

### 14.3 Money displays
```
Show four Moneys in a Grid columns 2:
- "1,247.50" USD size lg label "subtotal" tone ink
- "98.00" USD size lg label "tax" tone ink
- "1,345.50" USD size xl label "total due" tone accent
- "-150.00" USD size lg label "discount" tone error
```

### 14.4 HoursTable
```
Show a HoursTable for Blue Bottle Coffee:
Mon 7–18, Tue 7–18 (today), Wed 7–18, Thu 7–18, Fri 7–20, Sat 8–20,
Sun Closed.
```

### 14.5 Vote affordance under an answer
```
Show a TipCard with a useful answer, then below it an Inline with a
Vote id "ans1" ups 12 downs 1, and to the right a small Text "Was this
helpful?" tone muted.
```

### 14.6 Mixed fifth-wave — venue page
```
Build me a venue page for "Blue Bottle Coffee, Brooklyn":
- A Hero with image kicker "specialty coffee" title "Blue Bottle"
  subtitle "Single-origin, slow drip, sane prices."
- LocationCard with address + 0.4 mi distance + onTap directions
- OpenStatus open detail "Until 6pm"
- HoursTable for the full week
- Below, a Reaction bar (☕ 24, 🥐 8, 📚 5)
- A row of PersonChips for "Friends who go here" (3-4 chips with online
  rings).
```

## 15. Wave 6 — Travel

### 15.1 Boarding pass
```
Show me my boarding pass: passenger "Phuc Nguyen", origin "SGN" to
destination "JFK", depart 23:45, arrive "06:20+1", date "Mon 26 May",
flight VN98, seat 14A, gate B12, status "boarding".
```

### 15.2 Flight search results
```
Show three FlightSegments in a Stack as if these were search results
for SFO → JFK on May 30:
- Delta: 07:00 → 15:30, 5h 30m, nonstop
- United: 22:15 → 06:45+1, 5h 30m, 1 stop, "via ORD"
- JetBlue: 13:40 → 22:15, 5h 35m, nonstop
```

### 15.3 Multi-day itinerary
```
Build me a Stack with three ItineraryDay blocks for a long weekend in
Berlin (May 30 - June 1). Each day has 3-5 activities — mix transport,
food, sight, rest.
```

### 15.4 Hotel search list
```
Show a Stack of three HotelCards (use any hotel-y Unsplash URLs):
- "Hotel am Steinplatz", Charlottenburg, ★ 4.6, 187 reviews, €245
  with tags ["Breakfast", "Quiet"]
- "Michelberger Hotel", Friedrichshain, ★ 4.3, 412 reviews, €120 with
  tags ["Trendy"]
- "25hours Hotel Bikini", Zoo, ★ 4.5, 901 reviews, €180 with tags
  ["Pool", "View"]
```

### 15.5 Confirmation
```
Show a ConfirmationCard: title "Booking confirmed", confirmation
"BK-9MK4A2Z", subtitle "Hotel am Steinplatz, May 30 → Jun 1",
notes: ["Check-in: 3pm", "Free cancellation until May 28",
"Breakfast included"].
```

## 16. Wave 7 — Health

### 16.1 Workout session
```
Show three WorkoutSets stacked for today's session:
- Bench press, "Chest · barbell", 5 sets of 5 (60, 65, 70, 70 first 4
  done; last "AMRAP" at 70kg not done)
- Rows, "Back · cable", 4 sets of 8 at 45kg (all done)
- Squat, "Legs · barbell", 5x5 at 90kg (first 3 done)
```

### 16.2 Nutrition facts label
```
Show a NutritionFacts label for a serving of granola: 1 cup (228g),
2 servings/container, 250 cal, facts: Total Fat 12g (15%), Saturated
Fat 3g (15%) indented, Cholesterol 30mg (10%), Sodium 470mg (20%),
Carbs 31g (10%), Sugar 5g (10%) indented, Protein 5g.
```

### 16.3 Water tracker + sleep ring
```
Render a Stack with:
- A WaterTracker id "water" target 8 current 5, unitLabel "glasses"
- A SleepRing for last night: total "7h 24m", score 84, with stages
  Deep 18%, Light 52%, REM 22%, Awake 8%
- A HeartRateChart with currentBpm 72, readings [62, 64, 68, 70, 72,
  74, 80, 78, 75, 72, 70, 68, 72]
```

## 17. Wave 8 — Data

### 17.1 Leaderboard DataTable
```
Show a DataTable id "leaderboard" sortable=true with columns:
- "#" (key "rank", width 40)
- "Name" (key "name")
- "Streak" (key "streak", align center)
- "Score" (key "score", align end)
and 8 rows of plausible-looking made-up data.
```

### 17.2 File attachments
```
Show a Stack with five FileCards:
- "Q3-plan.pdf" 1.4 MB kind pdf, downloadable, onTap open_pdf
- "design-mockup.png" 4.7 MB kind image, onTap preview_img
- "budget.xlsx" 240 KB kind sheet, downloadable
- "recording.mp3" 18 MB kind audio
- "backup.zip" 124 MB kind zip, downloadable
```

### 17.3 JSON tree of an API payload
```
Show a JsonTree of this response:
{ id: 42, name: "Maria Chen", active: true, roles: ["admin", "design"],
  profile: { bio: "Design lead at Acme.", links: { twitter:
  "@maria", site: "maria.dev" } } }
Initially expanded.
```

### 17.4 Log stream
```
Show 7 LogLines for an API request lifecycle:
- 09:14:01.012 INFO  server "Request received POST /api/users"
- 09:14:01.018 DEBUG db "Acquired connection from pool"
- 09:14:01.024 INFO  validator "Body OK"
- 09:14:01.156 WARN  db "Slow query (132ms)"
- 09:14:01.198 INFO  server "Response 201"
- 09:14:01.201 DEBUG cache "Invalidated users:list"
- 09:14:01.220 ERROR webhook "Webhook delivery failed: timeout"
```

### 17.5 API call summary
```
Show an ApiResponse: POST /api/users, status 201, duration 184ms, body:
{ "id": 42, "name": "Maria Chen", "email": "maria@example.com",
  "createdAt": "2026-05-26T09:14:01.198Z" }
```

## 18. Wave 9 — Social

### 18.1 Post + Comments thread
```
Show a Post (Maria Chen @maria 2h: "Finally finished the redesign.
Three months of tiny tweaks. 🎉" with 24 likes (liked), 5 comments)
followed by three Comments:
- Alex, 1h, "This looks amazing — congrats!", 4 likes, liked false
- Sam, 45m, "How did you handle the color contrast issues?", 1 like,
  depth 1 (reply to Alex)
- Maria, 30m, "WCAG AAA for body text, AA for everything else", depth 1
```

### 18.2 Notifications inbox
```
Show four NotificationItems in a Stack:
- "Maria Chen replied to your comment" body "Loved the new charts in
  the dashboard.", time "2h", icon "share", tone info, unread true
- "Your weekly goal is met" body "You journaled every day this week.",
  time "5h", icon "check", tone success, unread true
- "Storage almost full" body "94% of 1 GB used", time "1d",
  icon "warning", tone warn, unread false
- "Sync failed" body "Will retry on next launch.", time "2d",
  icon "close", tone error, unread false
```

### 18.3 Activity feed
```
Show an ActivityFeed of 6 ActivityFeedItems (mix of verbs: commented
on, starred, joined, completed, closed, mentioned you in) for a
project workspace. Add previews on 3 of them.
```

### 18.4 Reading list
```
Show three BookmarkRows in a Stack (use any image-y Unsplash URLs):
- title "The hidden cost of meetings", source "NY Times", excerpt
  "What we lose when synchronous work crowds out deep work…", bookmarked
- title "Designing for trust", source "A List Apart", excerpt "Trust
  scales when patterns scale…", bookmarked
- title "Slow blogging", source "Robin Sloan's Newsletter", excerpt
  "On the joy of writing without an audience.", bookmarked false
```

## 19. Wave 10 — Music + Games

### 19.1 Album grid
```
Show 6 AlbumCards in a Grid columns=2: pick any iconic album titles +
artists, use any photo URLs, set showPlayOverlay=true on at least 2.
```

### 19.2 Tracklist
```
Show a Sheet containing 8 TrackRows for a tracklist:
1. "Bird at My Window" — Cassandra Wilson, 4:12, playing true
2. "Strange Fruit" — Cassandra Wilson, 5:42
3. "Last Train to Clarksville" — Cassandra Wilson, 5:08
...
(invent the rest)
```

### 19.3 Achievements list
```
Show four Achievements in a Stack:
- "10-day streak" 🔥 earned May 24, tier gold, "Logged a note every
  day for 10 days."
- "First essay" ✍️ earned Apr 12, tier silver, "Published your first
  long-form note."
- "Night owl" 🦉 earned Mar 30, tier bronze, "5 entries after 11pm."
- "100 essays" 💯 not earned, tier gold, earned false
```

### 19.4 ScoreBoard
```
Show a ScoreBoard for tonight's game: Brooklyn Nets 102, Boston Celtics
99, status "FINAL", meta "Barclays Center · 18,200 attended".
```

### 19.5 Level progress
```
Show a LevelProgress: level 7, xpCurrent 280, xpNeeded 500, title
"Habit Master".
```

### 19.6 Mixed waves 6-10 — multi-domain dashboard
```
Build me a wide-ranging dashboard combining the new components:
- Section "TRAVEL" with a BoardingPass and a small FlightSegment
- Section "HEALTH" with a WaterTracker (id "water" 5/8) and a
  SleepRing summary
- Section "TODAY" with three ActivityFeedItems
- Section "STATUS" with an ApiResponse showing a successful call and
  three LogLines.
```

## Filing issues

- If a component renders but looks off, screenshot + drop the JSON tree
  in the trace viewer.
- If a component is registered but the agent doesn't reach for it,
  shorten the description or add a user-phrasing example to the
  `description` string in the component file.
- If the creator agent renders prose instead of widgets, the preamble
  needs more emphasis on "use ui_render" — edit the kind-specific
  block in `creatorPreambleFor`.

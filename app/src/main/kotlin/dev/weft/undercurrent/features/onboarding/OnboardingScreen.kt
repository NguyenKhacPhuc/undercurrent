package dev.weft.undercurrent.features.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.android.routing.catalogFor
import dev.weft.contracts.ProviderKind
import dev.weft.undercurrent.features.personas.BuiltInPersonas
import dev.weft.undercurrent.features.personas.Persona
import dev.weft.undercurrent.theme.UndercurrentTheme

/**
 * First-launch onboarding. Four pages in a "calm, document-feel" voice:
 *  1. **Hi.** — Greeting + privacy promise rolled into one paragraph,
 *     plus a transition line that primes the user for the provider pick.
 *     Title is centered and oversized — the only page that gets the
 *     "big literary intro" treatment.
 *  2. **Memory you can see.** — Trust-building middle page. Explains
 *     the user's control over what the agent stores. Left-aligned title.
 *  3. **Pick a voice.** — Optional voice persona pick. Default is
 *     pre-selected; a small "Skip" link below the CTA falls back to
 *     Default explicitly. Persisted as the active voice on completion.
 *  4. **One last thing.** — Provider picker (2×2 grid of cards). Each
 *     card shows italic serif name + tone-of-voice description + model
 *     count. Selecting + tapping the bottom CTA advances to KeyPaste.
 *
 * Bottom CTA is full-width dark fill; small helper caption sits below it
 * (e.g. "Nothing happens until you tap." on page 1; "· Anthropic selected"
 * on the last page). The persona page is the only one with a Skip link.
 */
@Composable
internal fun OnboardingScreen(
    onComplete: (provider: ProviderKind, voicePersonaId: String) -> Unit,
) {
    var step by remember { mutableIntStateOf(0) }
    val pages = remember { onboardingPages() }
    var selectedProvider by remember { mutableStateOf(ProviderKind.Anthropic) }
    var selectedVoice by remember { mutableStateOf(BuiltInPersonas.Default) }

    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 24.dp)
            .padding(top = 20.dp, bottom = 24.dp),
    ) {
        // Page progress — three small dots in the top-left. Active dot is
        // ink; inactive dots are divider-colored.
        DotIndicator(
            currentStep = step,
            totalSteps = pages.size,
        )

        // Page content — fades between steps. Each page lays itself out;
        // the provider grid only appears on the last page.
        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "onboarding-step",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { activeStep ->
            OnboardingPageContent(
                page = pages[activeStep],
                selectedProvider = selectedProvider,
                onSelectProvider = { selectedProvider = it },
                selectedVoice = selectedVoice,
                onSelectVoice = { selectedVoice = it },
            )
        }

        // Bottom CTA + caption.
        val pageKind = pages[step].kind
        val ctaLabel = when (pageKind) {
            OnboardingPageKind.Intro -> "Begin reading →"
            OnboardingPageKind.ProviderPicker -> "Let's go →"
            else -> "Keep going →"
        }
        val helper = when (pageKind) {
            OnboardingPageKind.Intro -> "Nothing happens until you tap."
            OnboardingPageKind.PersonaPicker -> "· ${selectedVoice.name} selected"
            OnboardingPageKind.ProviderPicker -> "· ${selectedProvider.displayName()} selected"
            else -> null
        }

        PrimaryCta(
            label = ctaLabel,
            onClick = {
                if (step < pages.lastIndex) step++ else onComplete(selectedProvider, selectedVoice.id)
            },
        )
        if (helper != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = helper,
                style = typography.sansSmall.copy(color = colors.inkSubtle),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
        // Skip affordance — only on the persona page. Resets the voice to
        // Default (in case the user had tapped another option) and advances
        // past the picker without committing to a choice. Functionally the
        // same as tapping the CTA with Default selected, but reads more
        // honestly as "I'd rather not pick" than "I picked Default."
        if (pageKind == OnboardingPageKind.PersonaPicker) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Skip — use the default voice",
                style = typography.sansSmall.copy(color = colors.inkMuted),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedVoice = BuiltInPersonas.Default
                        if (step < pages.lastIndex) step++
                    }
                    .padding(vertical = 4.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Lays out one onboarding page. The intro page gets the oversized
 * centered title treatment; the rest use a smaller left-aligned title
 * with content stacked below. The persona / provider pages additionally
 * render their respective pickers below the body copy.
 */
@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    selectedProvider: ProviderKind,
    onSelectProvider: (ProviderKind) -> Unit,
    selectedVoice: Persona,
    onSelectVoice: (Persona) -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography

    Column(modifier = Modifier.fillMaxSize()) {
        if (page.kind == OnboardingPageKind.Intro) {
            // "Hi." style: title pushed into the upper-third, centered.
            Spacer(Modifier.weight(0.45f))
            Text(
                text = page.title,
                style = typography.serifBody.copy(
                    color = colors.ink,
                    fontSize = 80.sp,
                    lineHeight = 88.sp,
                    fontWeight = FontWeight.Normal,
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(0.3f))
            // Body sits in the bottom half — left-aligned for readability
            // even though the title above is centered.
            page.body.forEachIndexed { idx, paragraph ->
                Text(
                    text = paragraph,
                    style = typography.serifBodyLarge.copy(color = colors.ink),
                )
                if (idx < page.body.lastIndex) Spacer(Modifier.height(14.dp))
            }
            Spacer(Modifier.weight(0.4f))
        } else {
            // Subsequent pages: title sits at the top, left-aligned.
            Spacer(Modifier.height(28.dp))
            Text(
                text = page.title,
                style = typography.serifBody.copy(
                    color = colors.ink,
                    fontSize = 44.sp,
                    lineHeight = 50.sp,
                    fontWeight = FontWeight.Normal,
                ),
            )
            Spacer(Modifier.height(20.dp))
            page.body.forEachIndexed { idx, paragraph ->
                Text(
                    text = paragraph,
                    style = typography.serifBodyLarge.copy(color = colors.ink),
                )
                if (idx < page.body.lastIndex) Spacer(Modifier.height(12.dp))
            }
            when (page.kind) {
                OnboardingPageKind.PersonaPicker -> {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "VOICE",
                        style = typography.sansLabel.copy(color = colors.inkSubtle),
                    )
                    Spacer(Modifier.height(12.dp))
                    VoicePicker(
                        selected = selectedVoice,
                        onSelect = onSelectVoice,
                    )
                }
                OnboardingPageKind.ProviderPicker -> {
                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = "PROVIDER",
                        style = typography.sansLabel.copy(color = colors.inkSubtle),
                    )
                    Spacer(Modifier.height(12.dp))
                    ProviderPicker(
                        selected = selectedProvider,
                        onSelect = onSelectProvider,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Keys live on this device. Nothing else.",
                        style = typography.sansSmall.copy(
                            color = colors.inkMuted,
                            fontStyle = FontStyle.Italic,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
                else -> Unit
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

/**
 * Vertical list of selectable voice cards. Mirrors the [ProviderPicker]
 * affordances (2dp ink border on selected, surface fill, ✓ accent) but
 * lays out as a column of compact rows — voice descriptions are longer
 * than provider taglines, and five rows in a grid wouldn't divide evenly.
 *
 * Sourced from [BuiltInPersonas.Voices] so the picker always tracks the
 * shipped voice catalog without a duplicate list in onboarding.
 */
@Composable
private fun VoicePicker(
    selected: Persona,
    onSelect: (Persona) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BuiltInPersonas.Voices.forEach { persona ->
            VoiceCard(
                persona = persona,
                selected = persona.id == selected.id,
                onClick = { onSelect(persona) },
            )
        }
    }
}

@Composable
private fun VoiceCard(
    persona: Persona,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) colors.ink else Color.Transparent,
                shape = shapes.medium,
            )
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = persona.name,
                style = typography.serifBody.copy(
                    color = colors.ink,
                    fontSize = 17.sp,
                    fontStyle = FontStyle.Italic,
                ),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = persona.tagline,
                style = typography.serifBody.copy(
                    color = colors.inkMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                ),
            )
        }
        if (selected) {
            Spacer(Modifier.size(8.dp))
            Text(
                text = "✓",
                style = typography.sansHeader.copy(
                    color = colors.ink,
                    fontSize = 16.sp,
                ),
            )
        }
    }
}

/**
 * 2×2 grid of provider cards. Each row uses `IntrinsicSize.Max` so cards
 * in the same row share a height regardless of description length.
 */
@Composable
private fun ProviderPicker(
    selected: ProviderKind,
    onSelect: (ProviderKind) -> Unit,
) {
    // Group entries into rows of two. `chunked(2)` keeps the grid stable
    // if a 5th provider is ever added.
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ProviderKind.entries.chunked(2).forEach { rowProviders ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowProviders.forEach { provider ->
                    ProviderCard(
                        provider = provider,
                        selected = provider == selected,
                        onClick = { onSelect(provider) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
                // Pad lone trailing tile so it doesn't stretch full-width.
                if (rowProviders.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * One card in the provider grid.
 *
 * Layout:
 *  - Top row: italic serif provider name + ✓ when selected
 *  - Middle: serif description (the brand voice for each provider)
 *  - Bottom: "X MODELS  ›" in uppercase sans + chevron
 *
 * Selection state is the only border — unselected cards are borderless
 * but sit on a slightly elevated `surface` fill against the page bg.
 */
@Composable
private fun ProviderCard(
    provider: ProviderKind,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes

    Column(
        modifier = modifier
            .clip(shapes.medium)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) colors.ink else Color.Transparent,
                shape = shapes.medium,
            )
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = provider.displayName(),
                style = typography.serifBody.copy(
                    color = colors.ink,
                    fontSize = 19.sp,
                    fontStyle = FontStyle.Italic,
                ),
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Text(
                    text = "✓",
                    style = typography.sansHeader.copy(
                        color = colors.ink,
                        fontSize = 16.sp,
                    ),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = provider.tagline(),
            style = typography.serifBody.copy(
                color = colors.inkMuted,
                fontSize = 14.sp,
                lineHeight = 19.sp,
            ),
        )
        // Push the bottom row to the card's lower edge so all 4 cards in
        // the grid line up despite different description heights.
        Spacer(Modifier.weight(1f).heightIn(min = 24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = provider.modelCountLabel(),
                style = typography.sansLabel.copy(color = colors.inkSubtle),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "›",
                style = typography.sansHeader.copy(
                    color = colors.inkSubtle,
                    fontSize = 16.sp,
                ),
            )
        }
    }
}

/**
 * Small ink-on-bg dots in the top-left. 7dp circles, 6dp gap. Active dot
 * is full ink; inactive dots are divider-colored.
 */
@Composable
private fun DotIndicator(currentStep: Int, totalSteps: Int) {
    val colors = UndercurrentTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(totalSteps) { idx ->
            val active = idx == currentStep
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (active) colors.ink else colors.divider),
            )
        }
    }
}

/**
 * Full-width primary action button. Ink-filled (not accent) so the CTA
 * reads as "this is the way forward" even in palettes where accent is
 * loud (Newsprint red, etc.). Background text uses `colors.background`
 * for cross-palette safety.
 */
@Composable
private fun PrimaryCta(label: String, onClick: () -> Unit) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.large)
            .background(colors.ink)
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = typography.sansHeader.copy(
                color = colors.background,
                fontSize = 15.sp,
            ),
        )
    }
}

/**
 * Display name in the provider card header — kept short for the italic
 * serif treatment. (Note: the legacy "Anthropic" misspelling shown in
 * the design mockup is corrected here — the real brand is Anthropic.)
 */
private fun ProviderKind.displayName(): String = when (this) {
    ProviderKind.Anthropic -> "Anthropic"
    ProviderKind.OpenAI -> "OpenAI"
    ProviderKind.OpenRouter -> "OpenRouter"
    ProviderKind.DeepSeek -> "DeepSeek"
}

/**
 * Brand voice for each provider — the line under the name. Less about
 * accuracy, more about what kind of conversation you'll have if you pick
 * that backend.
 */
private fun ProviderKind.tagline(): String = when (this) {
    ProviderKind.Anthropic -> "Claude family. Slow, deliberate, well-read."
    ProviderKind.OpenAI -> "Familiar. Fast. The default everyone knows."
    ProviderKind.OpenRouter -> "Many models behind one key. Mix and match."
    ProviderKind.DeepSeek -> "Inexpensive, capable, and quiet."
}

/**
 * "N MODELS" / "40+ MODELS" footer label. Counts the SDK's curated
 * catalog for that provider — OpenRouter's catalog has many more behind
 * the scenes, so anything >= 20 is rendered as "20+ MODELS" so the
 * number doesn't lie about exact counts.
 */
private fun ProviderKind.modelCountLabel(): String {
    val n = catalogFor(this).size
    return when {
        n >= 40 -> "40+ MODELS"
        n >= 20 -> "20+ MODELS"
        else -> "$n MODELS"
    }
}

/**
 * Discriminator for an onboarding page. Drives layout (centered vs.
 * left-aligned title) and any picker that renders below the body copy.
 */
private enum class OnboardingPageKind { Intro, Memory, PersonaPicker, ProviderPicker }

private data class OnboardingPage(
    val kind: OnboardingPageKind,
    val title: String,
    val body: List<String>,
)

private fun onboardingPages(): List<OnboardingPage> = listOf(
    OnboardingPage(
        kind = OnboardingPageKind.Intro,
        title = "Hi.",
        body = listOf(
            "I'm Undercurrent. A small, private assistant that lives on this " +
                "phone. I'll help you think, write, remember — quietly, without " +
                "sending your conversations anywhere they don't need to go.",
            "I work best when you bring your own provider — Anthropic, OpenAI, " +
                "OpenRouter, DeepSeek. You'll paste a key in a minute. Until " +
                "then, this is just paper.",
        ),
    ),
    OnboardingPage(
        kind = OnboardingPageKind.Memory,
        title = "Memory you can see.",
        body = listOf(
            "If I remember something about you — a project name, a preference, " +
                "a way you like to be addressed — it'll show up in Memories. " +
                "Delete any of it. Wipe all of it.",
            "Conversations, traces, costs. Everything I've done is browsable. " +
                "Nothing's hidden, nothing leaves.",
        ),
    ),
    OnboardingPage(
        kind = OnboardingPageKind.PersonaPicker,
        title = "Pick a voice.",
        body = listOf(
            "Choose how I sound. Default works for most things — pick another " +
                "if you'd like a particular register. You can change this " +
                "anytime in Personas.",
        ),
    ),
    OnboardingPage(
        kind = OnboardingPageKind.ProviderPicker,
        title = "One last thing.",
        body = listOf(
            "Pick where the model lives. You can change this later — each " +
                "provider keeps its own key on this device.",
        ),
    ),
)

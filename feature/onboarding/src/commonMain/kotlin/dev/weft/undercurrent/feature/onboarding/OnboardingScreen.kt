package dev.weft.undercurrent.feature.onboarding

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
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.core.model.BuiltInPersonas
import dev.weft.undercurrent.core.model.Persona
import dev.weft.undercurrent.core.model.ProviderKind

/**
 * First-launch onboarding. Four pages in a "calm, document-feel"
 * voice: Hi → Memory you can see → Pick a voice → One last thing.
 *
 * KMP — commonMain. Moved from
 * `app/.../features/onboarding/OnboardingScreen.kt`. Adjustments:
 *   - `dev.weft.contracts.ProviderKind` → `:core:model` mirror.
 *   - `dev.weft.android.routing.catalogFor` is no longer reachable
 *     from commonMain; the host now passes a [modelCountFor] lambda
 *     that resolves to `ModelCatalog.modelsForProvider(p).size` on
 *     Android (and the StubModelCatalog returns 0 on iOS, which the
 *     screen still renders cleanly).
 *   - `BuiltInPersonas` / `Persona` from `:core:model`.
 *   - Theme imports from `:core:design-system`.
 */
@Composable
fun OnboardingScreen(
    modelCountFor: (ProviderKind) -> Int,
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
        DotIndicator(currentStep = step, totalSteps = pages.size)

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
                modelCountFor = modelCountFor,
            )
        }

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

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    selectedProvider: ProviderKind,
    onSelectProvider: (ProviderKind) -> Unit,
    selectedVoice: Persona,
    onSelectVoice: (Persona) -> Unit,
    modelCountFor: (ProviderKind) -> Int,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography

    Column(modifier = Modifier.fillMaxSize()) {
        if (page.kind == OnboardingPageKind.Intro) {
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
            page.body.forEachIndexed { idx, paragraph ->
                Text(
                    text = paragraph,
                    style = typography.serifBodyLarge.copy(color = colors.ink),
                )
                if (idx < page.body.lastIndex) Spacer(Modifier.height(14.dp))
            }
            Spacer(Modifier.weight(0.4f))
        } else {
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
                    VoicePicker(selected = selectedVoice, onSelect = onSelectVoice)
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
                        modelCountFor = modelCountFor,
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

@Composable
private fun ProviderPicker(
    selected: ProviderKind,
    onSelect: (ProviderKind) -> Unit,
    modelCountFor: (ProviderKind) -> Int,
) {
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
                        modelCount = modelCountFor(provider),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
                if (rowProviders.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProviderCard(
    provider: ProviderKind,
    selected: Boolean,
    onClick: () -> Unit,
    modelCount: Int,
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
        Spacer(Modifier.weight(1f).heightIn(min = 24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = modelCountLabel(modelCount),
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

private fun ProviderKind.displayName(): String = when (this) {
    ProviderKind.Anthropic -> "Anthropic"
    ProviderKind.OpenAI -> "OpenAI"
    ProviderKind.OpenRouter -> "OpenRouter"
    ProviderKind.DeepSeek -> "DeepSeek"
}

private fun ProviderKind.tagline(): String = when (this) {
    ProviderKind.Anthropic -> "Claude family. Slow, deliberate, well-read."
    ProviderKind.OpenAI -> "Familiar. Fast. The default everyone knows."
    ProviderKind.OpenRouter -> "Many models behind one key. Mix and match."
    ProviderKind.DeepSeek -> "Inexpensive, capable, and quiet."
}

private fun modelCountLabel(n: Int): String = when {
    n >= 40 -> "40+ MODELS"
    n >= 20 -> "20+ MODELS"
    else -> "$n MODELS"
}

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

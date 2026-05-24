package dev.weft.undercurrent.features.providers

import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLModel
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import dev.weft.undercurrent.ui.openInBrowser
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.android.routing.catalogFor
import dev.weft.android.routing.defaultPoolFor
import dev.weft.contracts.ProviderKind
import dev.weft.harness.agents.routing.ModelTier
import dev.weft.undercurrent.theme.UndercurrentTheme
import dev.weft.undercurrent.ui.ScaffoldTextAction
import dev.weft.undercurrent.ui.ScreenScaffold
import dev.weft.undercurrent.ui.SectionLabel
import dev.weft.undercurrent.ui.TipBox
import kotlinx.coroutines.launch

/**
 * Provider & model settings sub-screen. Drill-down from Settings → Provider.
 *
 * Layout:
 *  - **Providers** — one card per [ProviderKind]. Only the active card is
 *    expanded; inactive cards collapse to a single row (italic serif name
 *    + mono status subtitle + chevron). Tapping any card makes it active
 *    and expands it; the previously-active card collapses simultaneously.
 *    The expanded body contains the API key field (with eye-toggle for
 *    visibility), Save/Remove actions, and a tappable "Models
 *    customization" row that expands inline into per-tier dropdowns.
 *  - **Default model tier** — segmented control + a short TipBox.
 *
 * Active provider visual: 2dp ink border around the card. Mono subtitle
 * shows the masked stored key ("Active · •••• last4=A3xQ"). The 2dp
 * border + auto-expansion are the only "active" affordances — no
 * separate radio dot is needed.
 */
@Composable
internal fun ProvidersScreen(
    activeProvider: ProviderKind,
    defaultTier: ModelTier?,
    /**
     * Per-provider key status. Map entries with non-blank values are the
     * full stored key — we mask everything except last 4 here so the
     * secret stays the secret. (Map values were "•••• <last4>" in the
     * old screen; we now take the full last4 string and format it
     * locally for the new subtitle format.)
     */
    providerKeyStatus: Map<ProviderKind, String>,
    onProviderSelected: (ProviderKind) -> Unit,
    onProviderKeySaved: (ProviderKind, String) -> Unit,
    onProviderKeyRemoved: (ProviderKind) -> Unit,
    onDefaultTierSelected: (ModelTier?) -> Unit,
    getModelOverride: (ProviderKind, ModelTier) -> String?,
    onModelOverrideSelected: (ProviderKind, ModelTier, String?) -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(title = "Provider", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item("providers-label") {
                SectionLabel(text = "Providers")
            }
            items(ProviderKind.entries) { provider ->
                ProviderCard(
                    provider = provider,
                    active = provider == activeProvider,
                    storedKeyLast4 = providerKeyStatus[provider],
                    onTap = { onProviderSelected(provider) },
                    onKeySaved = { key -> onProviderKeySaved(provider, key) },
                    onKeyRemoved = { onProviderKeyRemoved(provider) },
                    getModelOverride = { tier -> getModelOverride(provider, tier) },
                    onModelOverrideSelected = { tier, id ->
                        onModelOverrideSelected(provider, tier, id)
                    },
                )
            }
            item("default-spacer") { Spacer(Modifier.height(20.dp)) }
            item("default-label") { SectionLabel(text = "Default model tier") }
            item("default-control") {
                TierSegmented(
                    selected = defaultTier,
                    onSelected = onDefaultTierSelected,
                )
            }
            item("default-tip") {
                Spacer(Modifier.height(6.dp))
                TipBox(
                    title = "About tiers",
                    text = "Cheap is fast and short. Standard is the default. " +
                        "Heavy is for hard problems. Auto picks per turn.",
                )
            }
        }
    }
}

/**
 * One provider card.
 *
 * Collapsed (when not active): italic serif name + mono subtitle
 * (key status) + right chevron. Borderless on the page background.
 *
 * Expanded (when active): same header + downward caret, plus the API
 * key section, Save / Remove inline actions, and a tappable Models
 * customization row that expands inline into per-tier dropdowns. 2dp
 * ink border highlights the active card.
 */
@Composable
private fun ProviderCard(
    provider: ProviderKind,
    active: Boolean,
    /**
     * Last 4 chars of the stored key (or null when no key is stored).
     * The cardHeader appends this to "•••• last4=" for the mono subtitle;
     * the field itself uses [PasswordVisualTransformation] for display.
     */
    storedKeyLast4: String?,
    onTap: () -> Unit,
    onKeySaved: (String) -> Unit,
    onKeyRemoved: () -> Unit,
    getModelOverride: (ModelTier) -> String?,
    onModelOverrideSelected: (ModelTier, String?) -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val shapes = UndercurrentTheme.shapes
    val typography = UndercurrentTheme.typography

    // Active = expanded. No local expansion state — tapping a card
    // activates it (parent dispatches the state change), and the visible
    // expanded body follows from there.
    val expanded = active

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .border(
                width = if (active) 2.dp else 0.dp,
                color = if (active) colors.ink else Color.Transparent,
                shape = shapes.medium,
            )
            .background(colors.surface),
    ) {
        // Header row — always visible.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTap)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.displayName(),
                    style = typography.serifBody.copy(
                        color = colors.ink,
                        fontSize = 22.sp,
                        fontStyle = FontStyle.Italic,
                    ),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitleFor(active, storedKeyLast4),
                    style = typography.mono.copy(
                        color = colors.inkMuted,
                        fontSize = 13.sp,
                    ),
                )
            }
            // Chevron — down when expanded, right when collapsed. Decorative
            // (the whole row is the tap target), but the rotation telegraphs
            // the affordance.
            Icon(
                imageVector = if (expanded) Icons.Default.ArrowDropDown
                else Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.inkMuted,
                modifier = Modifier.size(24.dp),
            )
        }
        // Expanded body — only rendered when active.
        if (expanded) {
            HorizontalThinDivider()
            ExpandedBody(
                provider = provider,
                storedKeyLast4 = storedKeyLast4,
                onKeySaved = onKeySaved,
                onKeyRemoved = onKeyRemoved,
                getModelOverride = getModelOverride,
                onModelOverrideSelected = onModelOverrideSelected,
            )
        }
    }
}

/**
 * Builds the mono subtitle line for a card header. Three states:
 *  - Active + key stored: "Active · •••• last4=A3xQ"
 *  - Inactive + key stored: "•••• last4=A3xQ"
 *  - No key: "No key saved"
 */
private fun subtitleFor(active: Boolean, last4: String?): String = when {
    last4 == null -> "No key saved"
    active -> "Active · •••• last4=$last4"
    else -> "•••• last4=$last4"
}

/**
 * Body shown only inside the active card — API key section, Save /
 * Remove inline actions, then the Models customization row (which
 * expands inline into per-tier dropdowns).
 */
@Composable
private fun ExpandedBody(
    provider: ProviderKind,
    storedKeyLast4: String?,
    onKeySaved: (String) -> Unit,
    onKeyRemoved: () -> Unit,
    getModelOverride: (ModelTier) -> String?,
    onModelOverrideSelected: (ModelTier, String?) -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val toolbarColorArgb = colors.surface.toArgb()

    var keyInput by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }
    var saveStatus by remember { mutableStateOf<SaveStatus>(SaveStatus.Idle) }
    var confirmRemove by remember { mutableStateOf(false) }
    var modelsExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        // API KEY label + small "Get key" deep link on the right. Same
        // CCT helper as KeyPasteScreen — useful here when a user is
        // adding a key for a second provider without backing out of
        // Settings.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "API KEY",
                style = typography.sansLabel.copy(color = colors.inkSubtle),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Get a key  →",
                style = typography.sansLabel.copy(
                    color = colors.ink,
                    fontWeight = FontWeight.SemiBold,
                ),
                modifier = Modifier
                    .clip(shapes.xsmall)
                    .clickable {
                        openInBrowser(context, provider.apiConsoleUrl(), toolbarColorArgb)
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.small)
                .border(width = 1.dp, color = colors.divider, shape = shapes.small)
                .background(colors.background)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = keyInput,
                    onValueChange = {
                        keyInput = it.trim()
                        if (saveStatus is SaveStatus.Invalid) saveStatus = SaveStatus.Idle
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = typography.mono.copy(color = colors.ink),
                    cursorBrush = SolidColor(colors.accent),
                    singleLine = true,
                    enabled = saveStatus !is SaveStatus.Checking,
                    visualTransformation = if (keyVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                )
                if (keyInput.isEmpty()) {
                    // Placeholder: hint at the format AND echo back the
                    // existing masked key so the user knows there's
                    // already one stored even when the field is empty.
                    val placeholder = if (storedKeyLast4 != null) {
                        "•••••••••••••••• $storedKeyLast4"
                    } else {
                        provider.keyPlaceholder()
                    }
                    Text(
                        text = placeholder,
                        style = typography.mono.copy(color = colors.inkSubtle),
                    )
                }
            }
            Icon(
                imageVector = if (keyVisible) Icons.Default.Visibility
                else Icons.Default.VisibilityOff,
                contentDescription = if (keyVisible) "Hide key" else "Show key",
                tint = colors.inkMuted,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { keyVisible = !keyVisible },
            )
        }
        if (saveStatus is SaveStatus.Invalid) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = (saveStatus as SaveStatus.Invalid).message,
                style = typography.sansSmall.copy(color = colors.error),
            )
        }
        Spacer(Modifier.height(12.dp))
        // Save + Remove inline actions. Save reads ink; Remove reads
        // error red — color is the only differentiator (no separate icons,
        // no buttons), matching the screenshot's plain-text style.
        Row(verticalAlignment = Alignment.CenterVertically) {
            val checking = saveStatus is SaveStatus.Checking
            InlineAction(
                label = if (checking) "Checking…" else "Save key",
                enabled = keyInput.isNotBlank() && !checking,
                isDestructive = false,
                onClick = {
                    val pending = keyInput
                    saveStatus = SaveStatus.Checking
                    scope.launch {
                        val result = dev.weft.undercurrent.features.providers.validateKey(provider, pending)
                        when (result) {
                            is ValidationResult.Ok -> {
                                onKeySaved(pending)
                                keyInput = ""
                                saveStatus = SaveStatus.Idle
                            }
                            is ValidationResult.Invalid -> {
                                saveStatus = SaveStatus.Invalid(result.message)
                            }
                        }
                    }
                },
            )
            if (storedKeyLast4 != null) {
                Spacer(Modifier.width(20.dp))
                InlineAction(
                    label = "Remove key",
                    enabled = !checking,
                    isDestructive = true,
                    onClick = { confirmRemove = true },
                )
            }
        }
    }
    HorizontalThinDivider()
    // Models customization row — collapsed by default, tap to inline-expand.
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { modelsExpanded = !modelsExpanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Models customization",
                    style = typography.sansHeader.copy(color = colors.ink, fontSize = 16.sp),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "4 tiers: Cheap · Standard · Vision · Heavy",
                    style = typography.sansSmall.copy(color = colors.inkMuted),
                )
            }
            Icon(
                imageVector = if (modelsExpanded) Icons.Default.ArrowDropDown
                else Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.inkMuted,
                modifier = Modifier.size(24.dp),
            )
        }
        if (modelsExpanded) {
            Spacer(Modifier.height(14.dp))
            ModelCustomizationGrid(
                provider = provider,
                getModelOverride = getModelOverride,
                onModelOverrideSelected = onModelOverrideSelected,
            )
        }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remove ${provider.displayName()} key?") },
            text = {
                Text(
                    "The stored key will be deleted from this device. " +
                        "Since ${provider.displayName()} is your active provider, " +
                        "you'll be returned to the key-paste screen to add a new one.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = false
                    onKeyRemoved()
                }) { Text("Remove", color = UndercurrentTheme.colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) { Text("Cancel") }
            },
        )
    }
}

private sealed interface SaveStatus {
    data object Idle : SaveStatus
    data object Checking : SaveStatus
    data class Invalid(val message: String) : SaveStatus
}

/**
 * Plain text action — used for "Save key" / "Remove key" / similar
 * inline action verbs. Destructive variant flips the color to `error`.
 * Disabled state dims the label to inkSubtle and stops the click.
 */
@Composable
private fun InlineAction(
    label: String,
    enabled: Boolean,
    isDestructive: Boolean,
    onClick: () -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val color = when {
        !enabled -> colors.inkSubtle
        isDestructive -> colors.error
        else -> colors.ink
    }
    Text(
        text = label,
        style = typography.sansHeader.copy(
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        ),
        modifier = Modifier
            .clip(UndercurrentTheme.shapes.xsmall)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
    )
}

@Composable
private fun HorizontalThinDivider() {
    val colors = UndercurrentTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.divider),
    )
}

/**
 * Per-provider model overrides. Four rows (cheap / standard / vision /
 * heavy), each a dropdown of the provider's catalog. Default selection
 * clears the override; picking another model writes it.
 */
@Composable
private fun ModelCustomizationGrid(
    provider: ProviderKind,
    getModelOverride: (ModelTier) -> String?,
    onModelOverrideSelected: (ModelTier, String?) -> Unit,
) {
    val defaults = remember(provider) { defaultPoolFor(provider) }
    val catalog = remember(provider) { catalogFor(provider) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ModelTier.entries.forEach { tier ->
            val currentId = getModelOverride(tier)
            val defaultModel = defaults.tierModel(tier)
            val resolvedModel = catalog.firstOrNull { it.id == currentId } ?: defaultModel
            ModelDropdown(
                tier = tier,
                selectedModel = resolvedModel,
                isDefault = currentId == null,
                defaultModel = defaultModel,
                catalog = catalog,
                onSelect = { picked ->
                    val newId = if (picked.id == defaultModel.id) null else picked.id
                    onModelOverrideSelected(tier, newId)
                },
            )
        }
    }
}

@Composable
private fun ModelDropdown(
    tier: ModelTier,
    selectedModel: LLModel,
    isDefault: Boolean,
    defaultModel: LLModel,
    catalog: List<LLModel>,
    onSelect: (LLModel) -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = tier.shortLabel(),
            style = typography.sansSmall.copy(color = colors.inkMuted),
            modifier = Modifier.width(72.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.small)
                    .border(width = 1.dp, color = colors.divider, shape = shapes.small)
                    .background(colors.background)
                    .clickable { menuOpen = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedModel.shortName(),
                        style = typography.serifBody.copy(color = colors.ink, fontSize = 14.sp),
                    )
                    val note = selectedModel.limitationNote(tier)
                    if (note != null || isDefault) {
                        Text(
                            text = listOfNotNull(
                                if (isDefault) "default" else null,
                                note,
                            ).joinToString(" · "),
                            style = typography.sansSmall.copy(color = colors.inkSubtle),
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = colors.inkMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = colors.surface,
            ) {
                catalog.forEach { model ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = model.shortName(),
                                    style = typography.serifBody.copy(color = colors.ink),
                                )
                                val note = listOfNotNull(
                                    if (model.id == defaultModel.id) "default" else null,
                                    model.limitationNote(tier),
                                ).joinToString(" · ")
                                if (note.isNotEmpty()) {
                                    Text(
                                        text = note,
                                        style = typography.sansSmall.copy(color = colors.inkSubtle),
                                    )
                                }
                            }
                        },
                        onClick = {
                            menuOpen = false
                            onSelect(model)
                        },
                    )
                }
            }
        }
    }
}

/** Helper: grab the right slot off a ModelPool. */
private fun dev.weft.harness.agents.routing.ModelPool.tierModel(tier: ModelTier): LLModel =
    when (tier) {
        ModelTier.Cheap -> cheap
        ModelTier.Standard -> standard
        ModelTier.Vision -> vision
        ModelTier.Heavy -> heavy
    }

private fun ModelTier.shortLabel(): String = when (this) {
    ModelTier.Cheap -> "Cheap"
    ModelTier.Standard -> "Standard"
    ModelTier.Vision -> "Vision"
    ModelTier.Heavy -> "Heavy"
}

private fun LLModel.shortName(): String = id

private fun LLModel.limitationNote(tier: ModelTier): String? {
    val caps = capabilities.orEmpty()
    val hasVision = caps.any { it is LLMCapability.Vision }
    val hasTools = caps.any { it == LLMCapability.Tools }
    return when {
        tier == ModelTier.Vision && !hasVision -> "no vision"
        !hasTools -> "no tools — limited agent use"
        else -> null
    }
}

@Composable
private fun TierSegmented(
    selected: ModelTier?,
    onSelected: (ModelTier?) -> Unit,
) {
    // Order: Auto / Cheap / Standard / Heavy. Vision is omitted from the
    // user picker — the router always selects vision when there's an
    // image attachment, regardless of the tier hint.
    val options = listOf<Pair<String, ModelTier?>>(
        "Auto" to null,
        "Cheap" to ModelTier.Cheap,
        "Standard" to ModelTier.Standard,
        "Heavy" to ModelTier.Heavy,
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { idx, (label, tier) ->
            SegmentedButton(
                selected = tier == selected,
                onClick = { onSelected(tier) },
                shape = SegmentedButtonDefaults.itemShape(index = idx, count = options.size),
                label = { Text(label) },
            )
        }
    }
}

private fun ProviderKind.displayName(): String = when (this) {
    ProviderKind.Anthropic -> "Anthropic"
    ProviderKind.OpenAI -> "OpenAI"
    ProviderKind.OpenRouter -> "OpenRouter"
    ProviderKind.DeepSeek -> "DeepSeek"
}

private fun ProviderKind.keyPlaceholder(): String = when (this) {
    ProviderKind.Anthropic -> "sk-ant-…"
    ProviderKind.OpenAI -> "sk-…"
    ProviderKind.OpenRouter -> "sk-or-…"
    ProviderKind.DeepSeek -> "sk-…"
}

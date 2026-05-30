package dev.weft.undercurrent.feature.providers

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.core.model.ModelTier
import dev.weft.undercurrent.core.model.ProviderKind
import dev.weft.undercurrent.core.model.apiConsoleUrl
import dev.weft.undercurrent.core.model.keyPlaceholder
import dev.weft.undercurrent.core.ui.ScreenScaffold
import dev.weft.undercurrent.core.ui.SectionLabel
import dev.weft.undercurrent.core.ui.TipBox
import dev.weft.undercurrent.shared.gateway.KeyValidationGateway
import dev.weft.undercurrent.shared.gateway.ModelCatalog
import dev.weft.undercurrent.shared.gateway.ModelInfo
import dev.weft.undercurrent.shared.gateway.ModelPool
import dev.weft.undercurrent.shared.gateway.ValidationResult
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Provider & model settings sub-screen. Drill-down from Settings →
 * Provider. One card per [ProviderKind]; the active card is expanded
 * with the API key field + Save/Remove + the models customization
 * row. Inactive cards collapse.
 *
 * KMP — commonMain. Moved from
 * `app/.../features/providers/ProvidersScreen.kt`. Adjustments:
 *   - `ai.koog.prompt.llm.LLModel` → [ModelInfo] mirror.
 *   - `dev.weft.android.routing.{catalogFor, defaultPoolFor}` →
 *     [ModelCatalog] gateway.
 *   - `validateKey` (Koog-backed, Android-only) → [KeyValidationGateway].
 *   - `openInBrowser` (CCT) lifted to [onOpenConsole] lambda.
 *   - Material icons-extended (Visibility, ArrowDropDown,
 *     KeyboardArrowRight) → Unicode glyphs + "Show"/"Hide" labels.
 *   - `dev.weft.harness.agents.routing.ModelTier` → `:core:model`
 *     mirror.
 *   - Imports from `:core:ui` / `:core:design-system`.
 */
@Composable
fun ProvidersScreen(
    activeProvider: ProviderKind,
    defaultTier: ModelTier?,
    /**
     * Per-provider key status. Map entries with non-blank values are
     * the last-4 of the stored key (already masked by the caller —
     * the real secret never reaches this screen).
     */
    providerKeyStatus: Map<ProviderKind, String>,
    modelCatalog: ModelCatalog,
    keyValidator: KeyValidationGateway,
    onProviderSelected: (ProviderKind) -> Unit,
    onProviderKeySaved: (ProviderKind, String) -> Unit,
    onProviderKeyRemoved: (ProviderKind) -> Unit,
    onDefaultTierSelected: (ModelTier?) -> Unit,
    getModelOverride: (ProviderKind, ModelTier) -> String?,
    onModelOverrideSelected: (ProviderKind, ModelTier, String?) -> Unit,
    onOpenConsole: (url: String) -> Unit,
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
                    modelCatalog = modelCatalog,
                    keyValidator = keyValidator,
                    onTap = { onProviderSelected(provider) },
                    onKeySaved = { key -> onProviderKeySaved(provider, key) },
                    onKeyRemoved = { onProviderKeyRemoved(provider) },
                    getModelOverride = { tier -> getModelOverride(provider, tier) },
                    onModelOverrideSelected = { tier, id ->
                        onModelOverrideSelected(provider, tier, id)
                    },
                    onOpenConsole = onOpenConsole,
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

@Composable
private fun ProviderCard(
    provider: ProviderKind,
    active: Boolean,
    storedKeyLast4: String?,
    modelCatalog: ModelCatalog,
    keyValidator: KeyValidationGateway,
    onTap: () -> Unit,
    onKeySaved: (String) -> Unit,
    onKeyRemoved: () -> Unit,
    getModelOverride: (ModelTier) -> String?,
    onModelOverrideSelected: (ModelTier, String?) -> Unit,
    onOpenConsole: (url: String) -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val shapes = UndercurrentTheme.shapes
    val typography = UndercurrentTheme.typography

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTap)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.displayName,
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
            // Chevron — ▾ when expanded, › when collapsed. Same Unicode
            // approach as ScreenScaffold's "←" back affordance.
            Text(
                text = if (expanded) "▾" else "›",
                style = typography.sansHeader.copy(
                    color = colors.inkMuted,
                    fontSize = 18.sp,
                ),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        if (expanded) {
            HorizontalThinDivider()
            ExpandedBody(
                provider = provider,
                storedKeyLast4 = storedKeyLast4,
                modelCatalog = modelCatalog,
                keyValidator = keyValidator,
                onKeySaved = onKeySaved,
                onKeyRemoved = onKeyRemoved,
                getModelOverride = getModelOverride,
                onModelOverrideSelected = onModelOverrideSelected,
                onOpenConsole = onOpenConsole,
            )
        }
    }
}

private fun subtitleFor(active: Boolean, last4: String?): String = when {
    last4 == null -> "No key saved"
    active -> "Active · •••• last4=$last4"
    else -> "•••• last4=$last4"
}

@Composable
private fun ExpandedBody(
    provider: ProviderKind,
    storedKeyLast4: String?,
    modelCatalog: ModelCatalog,
    keyValidator: KeyValidationGateway,
    onKeySaved: (String) -> Unit,
    onKeyRemoved: () -> Unit,
    getModelOverride: (ModelTier) -> String?,
    onModelOverrideSelected: (ModelTier, String?) -> Unit,
    onOpenConsole: (url: String) -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    val scope = rememberCoroutineScope()

    var keyInput by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }
    var saveStatus by remember { mutableStateOf<SaveStatus>(SaveStatus.Idle) }
    var confirmRemove by remember { mutableStateOf(false) }
    var modelsExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
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
                    .clickable { onOpenConsole(provider.apiConsoleUrl()) }
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
            Text(
                text = if (keyVisible) "Hide" else "Show",
                style = typography.sansLabel.copy(color = colors.inkMuted),
                modifier = Modifier
                    .clickable { keyVisible = !keyVisible }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
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
                        val result = keyValidator.validateKey(provider, pending)
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
            Text(
                text = if (modelsExpanded) "▾" else "›",
                style = typography.sansHeader.copy(
                    color = colors.inkMuted,
                    fontSize = 18.sp,
                ),
            )
        }
        if (modelsExpanded) {
            Spacer(Modifier.height(14.dp))
            ModelCustomizationGrid(
                provider = provider,
                modelCatalog = modelCatalog,
                getModelOverride = getModelOverride,
                onModelOverrideSelected = onModelOverrideSelected,
            )
        }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remove ${provider.displayName} key?") },
            text = {
                Text(
                    "The stored key will be deleted from this device. " +
                        "Since ${provider.displayName} is your active provider, " +
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

@Composable
private fun ModelCustomizationGrid(
    provider: ProviderKind,
    modelCatalog: ModelCatalog,
    getModelOverride: (ModelTier) -> String?,
    onModelOverrideSelected: (ModelTier, String?) -> Unit,
) {
    val defaults = remember(provider) { modelCatalog.defaultPoolForProvider(provider) }
    val catalog = remember(provider) { modelCatalog.modelsForProvider(provider) }

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
    selectedModel: ModelInfo,
    isDefault: Boolean,
    defaultModel: ModelInfo,
    catalog: List<ModelInfo>,
    onSelect: (ModelInfo) -> Unit,
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
                        text = selectedModel.shortName,
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
                Text(
                    text = "▾",
                    style = typography.sansLabel.copy(color = colors.inkMuted),
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
                                    text = model.shortName,
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

private fun ModelTier.shortLabel(): String = when (this) {
    ModelTier.Cheap -> "Cheap"
    ModelTier.Standard -> "Standard"
    ModelTier.Vision -> "Vision"
    ModelTier.Heavy -> "Heavy"
}

@Composable
private fun TierSegmented(
    selected: ModelTier?,
    onSelected: (ModelTier?) -> Unit,
) {
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

@Preview
@Composable
private fun ProvidersScreenPreview() {
    val stubCatalog = object : ModelCatalog {
        override fun modelsForProvider(provider: ProviderKind): List<ModelInfo> = when (provider) {
            ProviderKind.Anthropic -> listOf(
                ModelInfo("claude-sonnet-4-5", "Sonnet 4.5", hasVision = true, hasTools = true),
                ModelInfo("claude-haiku-4-5", "Haiku 4.5", hasVision = false, hasTools = true),
            )
            ProviderKind.OpenAI -> listOf(
                ModelInfo("gpt-5", "GPT-5", hasVision = true, hasTools = true),
                ModelInfo("gpt-5-mini", "GPT-5 Mini", hasVision = false, hasTools = true),
            )
            else -> emptyList()
        }
        override fun defaultPoolForProvider(provider: ProviderKind): ModelPool {
            val models = modelsForProvider(provider)
            val first = models.firstOrNull()
                ?: ModelInfo("none", "None", hasVision = false, hasTools = false)
            return ModelPool(
                cheap = first,
                standard = first,
                heavy = first,
                vision = models.firstOrNull { it.hasVision } ?: first,
            )
        }
    }
    val stubValidator = object : KeyValidationGateway {
        override suspend fun validateKey(
            provider: ProviderKind,
            apiKey: String,
        ): ValidationResult = ValidationResult.Ok
    }
    UndercurrentTheme {
        ProvidersScreen(
            activeProvider = ProviderKind.Anthropic,
            defaultTier = null,
            providerKeyStatus = mapOf(ProviderKind.Anthropic to "abcd"),
            modelCatalog = stubCatalog,
            keyValidator = stubValidator,
            onProviderSelected = {},
            onProviderKeySaved = { _, _ -> },
            onProviderKeyRemoved = {},
            onDefaultTierSelected = {},
            getModelOverride = { _, _ -> null },
            onModelOverrideSelected = { _, _, _ -> },
            onOpenConsole = {},
            onBack = {},
        )
    }
}

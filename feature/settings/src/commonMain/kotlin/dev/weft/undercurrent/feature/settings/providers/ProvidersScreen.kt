package dev.weft.undercurrent.feature.settings.providers

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import dev.weft.undercurrent.core.resources.Res
import dev.weft.undercurrent.core.resources.common_cancel
import dev.weft.undercurrent.core.resources.model_tier_cheap
import dev.weft.undercurrent.core.resources.model_tier_heavy
import dev.weft.undercurrent.core.resources.model_tier_standard
import dev.weft.undercurrent.core.resources.model_tier_vision
import dev.weft.undercurrent.core.resources.providers_active_key_format
import dev.weft.undercurrent.core.resources.providers_api_key_label
import dev.weft.undercurrent.core.resources.providers_checking
import dev.weft.undercurrent.core.resources.providers_default_tag
import dev.weft.undercurrent.core.resources.providers_get_key
import dev.weft.undercurrent.core.resources.providers_hide
import dev.weft.undercurrent.core.resources.providers_models_customization
import dev.weft.undercurrent.core.resources.providers_models_subtitle
import dev.weft.undercurrent.core.resources.providers_no_key
import dev.weft.undercurrent.core.resources.providers_remove
import dev.weft.undercurrent.core.resources.providers_remove_confirm_body_format
import dev.weft.undercurrent.core.resources.providers_remove_confirm_title_format
import dev.weft.undercurrent.core.resources.providers_remove_key
import dev.weft.undercurrent.core.resources.providers_save_key
import dev.weft.undercurrent.core.resources.providers_section_default_tier
import dev.weft.undercurrent.core.resources.providers_section_providers
import dev.weft.undercurrent.core.resources.providers_show
import dev.weft.undercurrent.core.resources.providers_stored_key_format
import dev.weft.undercurrent.core.resources.providers_tier_auto
import dev.weft.undercurrent.core.resources.providers_tip_about_tiers
import dev.weft.undercurrent.core.resources.providers_tip_about_tiers_title
import dev.weft.undercurrent.core.resources.providers_title
import dev.weft.undercurrent.core.ui.ScreenScaffold
import dev.weft.undercurrent.core.ui.SectionLabel
import dev.weft.undercurrent.core.ui.TipBox
import dev.weft.undercurrent.core.domain.ModelInfo
import dev.weft.undercurrent.core.domain.ModelPool
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Provider & model settings sub-screen. Drill-down from Settings →
 * Provider. One card per [ProviderKind]; the active card is expanded
 * with the API key field + Save/Remove + the models customization
 * row. Inactive cards collapse.
 *
 * Stateless: everything is read from [ProviderState] and every action
 * is a callback. The owning [ProviderViewModel] holds the repositories
 * and validation logic — this screen never touches them.
 */
@Composable
fun ProvidersScreen(
    state: ProviderState,
    onProviderSelected: (ProviderKind) -> Unit = {},
    onSaveKey: (ProviderKind, String) -> Unit = { _, _ -> },
    onKeyInputChanged: () -> Unit = {},
    onProviderKeyRemoved: (ProviderKind) -> Unit = {},
    onDefaultTierSelected: (ModelTier?) -> Unit = {},
    onModelOverrideSelected: (ProviderKind, ModelTier, String?) -> Unit = { _, _, _ -> },
    onOpenConsole: (url: String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    ScreenScaffold(title = stringResource(Res.string.providers_title), onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item("providers-label") {
                SectionLabel(text = stringResource(Res.string.providers_section_providers))
            }
            items(ProviderKind.entries) { provider ->
                ProviderCard(
                    provider = provider,
                    active = provider == state.activeProvider,
                    storedKey = state.keyStatus[provider],
                    models = state.modelsFor(provider),
                    keyValidation = state.keyValidation,
                    overrideFor = { tier -> state.overrideFor(provider, tier) },
                    onTap = { onProviderSelected(provider) },
                    onSaveKey = { key -> onSaveKey(provider, key) },
                    onKeyInputChanged = onKeyInputChanged,
                    onKeyRemoved = { onProviderKeyRemoved(provider) },
                    onModelOverrideSelected = { tier, id ->
                        onModelOverrideSelected(provider, tier, id)
                    },
                    onOpenConsole = onOpenConsole,
                )
            }
            item("default-spacer") { Spacer(Modifier.height(20.dp)) }
            item("default-label") { SectionLabel(text = stringResource(Res.string.providers_section_default_tier)) }
            item("default-control") {
                TierSegmented(
                    selected = state.defaultTier,
                    onSelected = onDefaultTierSelected,
                )
            }
            item("default-tip") {
                Spacer(Modifier.height(6.dp))
                TipBox(
                    title = stringResource(Res.string.providers_tip_about_tiers_title),
                    text = stringResource(Res.string.providers_tip_about_tiers),
                )
            }
        }
    }
}

@Composable
private fun ProviderCard(
    provider: ProviderKind,
    active: Boolean,
    storedKey: String?,
    models: ProviderModels?,
    keyValidation: KeyValidationStatus,
    overrideFor: (ModelTier) -> String?,
    onTap: () -> Unit,
    onSaveKey: (String) -> Unit,
    onKeyInputChanged: () -> Unit,
    onKeyRemoved: () -> Unit,
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
                    text = subtitleFor(active, storedKey),
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
                storedKey = storedKey,
                models = models,
                keyValidation = keyValidation,
                overrideFor = overrideFor,
                onSaveKey = onSaveKey,
                onKeyInputChanged = onKeyInputChanged,
                onKeyRemoved = onKeyRemoved,
                onModelOverrideSelected = onModelOverrideSelected,
                onOpenConsole = onOpenConsole,
            )
        }
    }
}

@Composable
private fun subtitleFor(active: Boolean, last4: String?): String = when {
    last4 == null -> stringResource(Res.string.providers_no_key)
    active -> stringResource(Res.string.providers_active_key_format, last4)
    else -> stringResource(Res.string.providers_stored_key_format, last4)
}

@Composable
private fun ExpandedBody(
    provider: ProviderKind,
    storedKey: String?,
    models: ProviderModels?,
    keyValidation: KeyValidationStatus,
    overrideFor: (ModelTier) -> String?,
    onSaveKey: (String) -> Unit,
    onKeyInputChanged: () -> Unit,
    onKeyRemoved: () -> Unit,
    onModelOverrideSelected: (ModelTier, String?) -> Unit,
    onOpenConsole: (url: String) -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes

    var keyInput by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    var modelsExpanded by remember { mutableStateOf(false) }
    var wasChecking by remember { mutableStateOf(false) }

    // Clear the field only after a successful save (Checking -> Idle).
    // A bare "idle => clear" would wipe input mid-typing, since editing
    // an invalid key dispatches ClearKeyValidation (-> Idle).
    LaunchedEffect(keyValidation) {
        when (keyValidation) {
            KeyValidationStatus.Checking -> wasChecking = true
            KeyValidationStatus.Idle -> if (wasChecking) {
                keyInput = ""
                wasChecking = false
            }
            is KeyValidationStatus.Invalid -> wasChecking = false
        }
    }

    val checking = keyValidation is KeyValidationStatus.Checking

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.providers_api_key_label),
                style = typography.sansLabel.copy(color = colors.inkSubtle),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(Res.string.providers_get_key),
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
                        if (keyValidation is KeyValidationStatus.Invalid) onKeyInputChanged()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = typography.mono.copy(color = colors.ink),
                    cursorBrush = SolidColor(colors.accent),
                    singleLine = true,
                    enabled = !checking,
                    visualTransformation = if (keyVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                )
                if (keyInput.isEmpty()) {
                    val placeholder = if (storedKey != null) {
                        "•••••••••••••••• $storedKey"
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
                text = if (keyVisible) stringResource(Res.string.providers_hide) else stringResource(Res.string.providers_show),
                style = typography.sansLabel.copy(color = colors.inkMuted),
                modifier = Modifier
                    .clickable { keyVisible = !keyVisible }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        if (keyValidation is KeyValidationStatus.Invalid) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = keyValidation.message,
                style = typography.sansSmall.copy(color = colors.error),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            InlineAction(
                label = if (checking) stringResource(Res.string.providers_checking) else stringResource(Res.string.providers_save_key),
                enabled = keyInput.isNotBlank() && !checking,
                isDestructive = false,
                onClick = { onSaveKey(keyInput) },
            )
            if (storedKey != null) {
                Spacer(Modifier.width(20.dp))
                InlineAction(
                    label = stringResource(Res.string.providers_remove_key),
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
                    text = stringResource(Res.string.providers_models_customization),
                    style = typography.sansHeader.copy(color = colors.ink, fontSize = 16.sp),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(Res.string.providers_models_subtitle),
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
        if (modelsExpanded && models != null) {
            Spacer(Modifier.height(14.dp))
            ModelCustomizationGrid(
                models = models,
                overrideFor = overrideFor,
                onModelOverrideSelected = onModelOverrideSelected,
            )
        }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(stringResource(Res.string.providers_remove_confirm_title_format, provider.displayName)) },
            text = {
                Text(stringResource(Res.string.providers_remove_confirm_body_format, provider.displayName))
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = false
                    onKeyRemoved()
                }) { Text(stringResource(Res.string.providers_remove), color = UndercurrentTheme.colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) { Text(stringResource(Res.string.common_cancel)) }
            },
        )
    }
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
    models: ProviderModels,
    overrideFor: (ModelTier) -> String?,
    onModelOverrideSelected: (ModelTier, String?) -> Unit,
) {
    val defaults = models.defaultPool
    val catalog = models.models

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ModelTier.entries.forEach { tier ->
            val currentId = overrideFor(tier)
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
            text = stringResource(tier.shortLabelRes()),
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
                        val defaultTag = stringResource(Res.string.providers_default_tag)
                        Text(
                            text = listOfNotNull(
                                if (isDefault) defaultTag else null,
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
                                val defaultTag = stringResource(Res.string.providers_default_tag)
                                val note = listOfNotNull(
                                    if (model.id == defaultModel.id) defaultTag else null,
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

private fun ModelTier.shortLabelRes(): StringResource = when (this) {
    ModelTier.Cheap -> Res.string.model_tier_cheap
    ModelTier.Standard -> Res.string.model_tier_standard
    ModelTier.Vision -> Res.string.model_tier_vision
    ModelTier.Heavy -> Res.string.model_tier_heavy
}

@Composable
private fun TierSegmented(
    selected: ModelTier?,
    onSelected: (ModelTier?) -> Unit,
) {
    val options = listOf<Pair<String, ModelTier?>>(
        stringResource(Res.string.providers_tier_auto) to null,
        stringResource(Res.string.model_tier_cheap) to ModelTier.Cheap,
        stringResource(Res.string.model_tier_standard) to ModelTier.Standard,
        stringResource(Res.string.model_tier_heavy) to ModelTier.Heavy,
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
    val anthropicModels = listOf(
        ModelInfo("claude-sonnet-4-5", "Sonnet 4.5", hasVision = true, hasTools = true),
        ModelInfo("claude-haiku-4-5", "Haiku 4.5", hasVision = false, hasTools = true),
    )
    val pool = ModelPool(
        cheap = anthropicModels[1],
        standard = anthropicModels[0],
        heavy = anthropicModels[0],
        vision = anthropicModels[0],
    )
    UndercurrentTheme {
        ProvidersScreen(
            state = ProviderState(
                activeProvider = ProviderKind.Anthropic,
                defaultTier = null,
                keyStatus = mapOf(ProviderKind.Anthropic to "•••"),
                catalogs = mapOf(
                    ProviderKind.Anthropic to ProviderModels(anthropicModels, pool),
                ),
            ),
        )
    }
}

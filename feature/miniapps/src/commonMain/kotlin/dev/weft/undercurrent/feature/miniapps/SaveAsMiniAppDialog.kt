package dev.weft.undercurrent.feature.miniapps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.core.model.MiniApp
import dev.weft.undercurrent.core.resources.Res
import dev.weft.undercurrent.core.resources.common_cancel
import dev.weft.undercurrent.core.resources.common_create
import dev.weft.undercurrent.core.resources.common_delete
import dev.weft.undercurrent.core.resources.common_save
import dev.weft.undercurrent.core.resources.miniapps_dialog_delete_body
import dev.weft.undercurrent.core.resources.miniapps_dialog_delete_title
import dev.weft.undercurrent.core.resources.miniapps_dialog_field_icon
import dev.weft.undercurrent.core.resources.miniapps_dialog_field_icon_placeholder
import dev.weft.undercurrent.core.resources.miniapps_dialog_field_name
import dev.weft.undercurrent.core.resources.miniapps_dialog_field_name_placeholder
import dev.weft.undercurrent.core.resources.miniapps_dialog_field_prompt
import dev.weft.undercurrent.core.resources.miniapps_dialog_field_prompt_placeholder
import dev.weft.undercurrent.core.resources.miniapps_dialog_hint
import dev.weft.undercurrent.core.resources.miniapps_dialog_title_create
import dev.weft.undercurrent.core.resources.miniapps_dialog_title_edit
import org.jetbrains.compose.resources.stringResource

/**
 * Dialog that captures a prompt as a [MiniApp]. Two modes — create
 * (initial null) and edit (initial non-null).
 *
 * KMP — commonMain. Moved from
 * `app/.../features/miniapps/SaveAsMiniAppDialog.kt`. No behavioral
 * changes; just the import paths follow the migrated modules.
 */
@Composable
fun SaveAsMiniAppDialog(
    initial: MiniApp?,
    suggestedPrompt: String,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, triggerPrompt: String) -> Unit,
    onDelete: (() -> Unit)? = null,
    onSetApprovedScopes: ((Set<String>) -> Unit)? = null,
) {
    val colors = UndercurrentTheme.colors

    var name by remember {
        mutableStateOf(initial?.name ?: suggestSlugFromPrompt(suggestedPrompt))
    }
    var emoji by remember { mutableStateOf(initial?.emoji ?: DEFAULT_EMOJI) }
    var prompt by remember { mutableStateOf(initial?.triggerPrompt ?: suggestedPrompt) }
    var confirmDelete by remember { mutableStateOf(false) }
    var approved by remember { mutableStateOf(initial?.approvedScopes ?: emptySet()) }

    val isEdit = initial != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEdit) stringResource(Res.string.miniapps_dialog_title_edit)
                else stringResource(Res.string.miniapps_dialog_title_create),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LabeledField(
                        label = stringResource(Res.string.miniapps_dialog_field_icon),
                        value = emoji,
                        onValueChange = { v -> emoji = v.take(4) },
                        placeholder = stringResource(Res.string.miniapps_dialog_field_icon_placeholder),
                        modifier = Modifier.width(72.dp),
                    )
                    LabeledField(
                        label = stringResource(Res.string.miniapps_dialog_field_name),
                        value = name,
                        onValueChange = { name = it },
                        placeholder = stringResource(Res.string.miniapps_dialog_field_name_placeholder),
                        modifier = Modifier.weight(1f),
                    )
                }
                LabeledField(
                    label = stringResource(Res.string.miniapps_dialog_field_prompt),
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = stringResource(Res.string.miniapps_dialog_field_prompt_placeholder),
                    minLines = 4,
                )
                Text(
                    text = stringResource(Res.string.miniapps_dialog_hint),
                    style = UndercurrentTheme.typography.sansSmall.copy(
                        color = colors.inkSubtle,
                    ),
                )
                if (isEdit && initial?.htmlDocument != null && onSetApprovedScopes != null) {
                    val actions = initial.requestedActions(OfferableActions.readMostlyDefaults())
                    if (actions.isNotEmpty()) {
                        Text(
                            text = "Permissions",
                            style = UndercurrentTheme.typography.sansSmall.copy(color = colors.inkSubtle),
                        )
                        actions.forEach { action ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = action.description.ifBlank { action.name },
                                    style = UndercurrentTheme.typography.sansSmall,
                                    modifier = Modifier.weight(1f),
                                )
                                Switch(
                                    checked = action.name in approved,
                                    onCheckedChange = { granted ->
                                        approved = if (granted) approved + action.name else approved - action.name
                                        onSetApprovedScopes(approved)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && prompt.isNotBlank(),
                onClick = {
                    onSave(
                        name.trim(),
                        emoji.ifBlank { DEFAULT_EMOJI },
                        prompt.trim(),
                    )
                },
            ) {
                Text(
                    if (isEdit) stringResource(Res.string.common_save)
                    else stringResource(Res.string.common_create),
                )
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text(stringResource(Res.string.common_delete), color = colors.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_cancel)) }
            }
        },
    )

    if (confirmDelete && onDelete != null && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(Res.string.miniapps_dialog_delete_title, initial.name)) },
            text = { Text(stringResource(Res.string.miniapps_dialog_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(stringResource(Res.string.common_delete), color = colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(Res.string.common_cancel)) }
            },
        )
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int = 1,
    modifier: Modifier = Modifier,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = typography.sansLabel.copy(color = colors.inkSubtle),
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.small)
                .border(width = 1.dp, color = colors.divider, shape = shapes.small)
                .background(colors.surface)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = typography.serifBody.copy(
                    color = colors.ink,
                    fontSize = 16.sp,
                ),
                cursorBrush = SolidColor(colors.accent),
                minLines = minLines,
            )
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = typography.serifBody.copy(
                        color = colors.inkSubtle,
                        fontSize = 16.sp,
                    ),
                )
            }
        }
    }
}

private const val DEFAULT_EMOJI = "✨"

/**
 * Heuristic name suggestion from the prompt. Leading 3-4 words,
 * trimmed of trailing punctuation, Title-Cased.
 */
private fun suggestSlugFromPrompt(prompt: String): String {
    val words = prompt.trim()
        .split(Regex("\\s+"))
        .take(4)
        .map { it.trim().trimEnd(',', '.', '?', '!', ':', ';') }
        .filter { it.isNotEmpty() }
    if (words.isEmpty()) return ""
    return words.joinToString(" ") { word ->
        word.replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase() else ch.toString() }
    }
}

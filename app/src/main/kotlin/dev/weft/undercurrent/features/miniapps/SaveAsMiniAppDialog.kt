package dev.weft.undercurrent.features.miniapps

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
import dev.weft.undercurrent.theme.UndercurrentTheme

/**
 * Dialog that captures a prompt as a [MiniApp].
 *
 * Used in two contexts:
 *  - **Create mode** ([initial] is null): launched from the "Save as
 *    mini-app" affordance on a successful assistant reply. The prompt
 *    is pre-filled with the user's preceding message; name + emoji
 *    are blank with placeholder hints. The user can edit any field.
 *  - **Edit mode** ([initial] is non-null): launched from the
 *    management screen via tap on a mini-app card. Fields prefilled;
 *    a Delete button appears alongside Cancel.
 *
 * Validation: name + prompt must be non-blank to save. Emoji defaults
 * to a single sparkle if the user leaves it blank — we'd rather drop
 * a recognizable placeholder than refuse to save.
 */
@Composable
internal fun SaveAsMiniAppDialog(
    initial: MiniApp?,
    suggestedPrompt: String,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, triggerPrompt: String) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val colors = UndercurrentTheme.colors

    var name by remember {
        // Auto-suggest a name from the first few words of the prompt
        // so the user often only has to confirm. Tap-to-edit if they
        // want to change it.
        mutableStateOf(initial?.name ?: suggestSlugFromPrompt(suggestedPrompt))
    }
    var emoji by remember { mutableStateOf(initial?.emoji ?: DEFAULT_EMOJI) }
    var prompt by remember { mutableStateOf(initial?.triggerPrompt ?: suggestedPrompt) }
    var confirmDelete by remember { mutableStateOf(false) }

    val isEdit = initial != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit mini-app" else "Save as mini-app") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Emoji + name on one row — emoji is a tight tile,
                // name takes the rest. Matches the chip layout the
                // user will see in the Add-to-Chat sheet.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LabeledField(
                        label = "Icon",
                        value = emoji,
                        onValueChange = { v ->
                            // Cap to a small handful of code points —
                            // single emoji / letter / kanji. Anything
                            // longer would overflow the chip.
                            emoji = v.take(4)
                        },
                        placeholder = "✨",
                        modifier = Modifier.width(72.dp),
                    )
                    LabeledField(
                        label = "Name",
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "Log water",
                        modifier = Modifier.weight(1f),
                    )
                }
                LabeledField(
                    label = "Prompt",
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = "What this mini-app should ask the assistant.",
                    minLines = 4,
                )
                Text(
                    text = "Tapping this mini-app later will send the prompt above " +
                        "to the assistant — exactly as you typed it.",
                    style = UndercurrentTheme.typography.sansSmall.copy(
                        color = colors.inkSubtle,
                    ),
                )
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
            ) { Text(if (isEdit) "Save" else "Create") }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("Delete", color = colors.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )

    if (confirmDelete && onDelete != null && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete \"${initial.name}\"?") },
            text = { Text("The mini-app will be permanently removed. Existing chats are not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete", color = colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
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

/** Default emoji when the user leaves the slot empty. */
private const val DEFAULT_EMOJI = "✨"

/**
 * Heuristic name suggestion from the prompt. We grab the leading
 * 3-4 words, trim punctuation, and Title-Case the result. Good
 * enough for ~70% of cases; the user can always edit.
 *
 * Examples:
 *  - "Log 8oz of water and tell me today's total" → "Log 8oz Of Water"
 *  - "What's the weather in Portland?"            → "What's The Weather"
 *  - "Summarize today's calendar"                 → "Summarize Today's Calendar"
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

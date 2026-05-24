package dev.weft.undercurrent.features.personas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.undercurrent.theme.UndercurrentTheme
import dev.weft.undercurrent.ui.ScreenScaffold
import org.koin.androidx.compose.koinViewModel

/**
 * Persona picker — the user picks one "voice" that injects per-turn
 * instructions into the agent's system prompt. Two sections — built-in
 * (always present) and custom (user-created) — plus a "+ New persona"
 * tile at the bottom of the custom section.
 *
 * Interaction model:
 *  - **Tap** any row → make it active. The next assistant turn picks it
 *    up via the runtime's `extraVolatilePrefix` lambda (no rebuild).
 *  - **Long-press** a custom row → open the editor dialog (rename,
 *    rewrite, or delete). Built-ins ignore long-press.
 *  - **+ New persona** tile → editor dialog in create mode.
 *  - **+ New** in the header trailing → same as the tile.
 *
 * Visual: each row is a card with a letter-avatar tile on the left, the
 * persona name + tagline in the middle, a status pill on the right
 * (ACTIVE / BUILT-IN / CUSTOM). Active row gets a 2dp ink border.
 */
@Composable
internal fun PersonasScreen(
    onBack: () -> Unit,
    vm: PersonasViewModel = koinViewModel(),
) {
    val activeVoice by vm.activeVoice.collectAsState()
    val activeRole by vm.activeRole.collectAsState()
    val customPersonas by vm.customPersonas.collectAsState()

    // Helper — is this persona currently active in either slot?
    val activeVoiceId = activeVoice.id
    val activeRoleId = activeRole?.id

    // Editor state — null means closed; non-null means "open in mode X".
    var editorMode by remember { mutableStateOf<EditorMode?>(null) }

    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography

    // Partition custom personas by kind so they appear in the right
    // section. Legacy customs (kind = Custom, pre-dual-slot) are treated
    // as voices — that was the only slot before the role concept landed.
    val customVoices = remember(customPersonas) {
        customPersonas.filter { it.kind != PersonaKind.Role }
    }
    val customRoles = remember(customPersonas) {
        customPersonas.filter { it.kind == PersonaKind.Role }
    }

    ScreenScaffold(
        title = "Personas",
        onBack = onBack,
        // No global "+ New" trailing — the per-section "+ New" buttons
        // below handle adding (and are unambiguous about which slot the
        // new persona will fill).
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().weight(1f),
        ) {
            // Italic-serif intro paragraph — the screen's own brief.
            // Mentions both axes so the user understands what they're
            // choosing between: a writing style (voice) or a domain of
            // expertise (role).
            item("intro") {
                Text(
                    text = "A voice shapes how I sound. A role shapes what I focus on. " +
                        "You can pick one of each — they layer.",
                    style = typography.serifBody.copy(
                        color = colors.ink,
                        fontStyle = FontStyle.Italic,
                        fontSize = 17.sp,
                        lineHeight = 26.sp,
                    ),
                    modifier = Modifier.padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 14.dp,
                        bottom = 28.dp,
                    ),
                )
            }
            // ─── Voices section ──────────────────────────────────────
            item("voices-label") {
                SectionHeader(
                    label = "Voices",
                    onAdd = { editorMode = EditorMode.New(PersonaKind.Voice) },
                )
            }
            items(BuiltInPersonas.Voices, key = { it.id }) { persona ->
                PersonaCard(
                    persona = persona,
                    active = persona.id == activeVoiceId || persona.id == activeRoleId,
                    onTap = { vm.onPersonaTap(persona) },
                    onLongPress = null, // built-ins are immutable
                )
            }
            items(customVoices, key = { it.id }) { persona ->
                PersonaCard(
                    persona = persona,
                    active = persona.id == activeVoiceId || persona.id == activeRoleId,
                    onTap = { vm.onPersonaTap(persona) },
                    onLongPress = { editorMode = EditorMode.Edit(persona) },
                )
            }
            // ─── Roles section ───────────────────────────────────────
            item("roles-label") {
                Spacer(Modifier.height(24.dp))
                SectionHeader(
                    label = "Roles",
                    onAdd = { editorMode = EditorMode.New(PersonaKind.Role) },
                )
            }
            items(BuiltInPersonas.Roles, key = { it.id }) { persona ->
                PersonaCard(
                    persona = persona,
                    active = persona.id == activeVoiceId || persona.id == activeRoleId,
                    onTap = { vm.onPersonaTap(persona) },
                    onLongPress = null, // built-ins are immutable
                )
            }
            items(customRoles, key = { it.id }) { persona ->
                PersonaCard(
                    persona = persona,
                    active = persona.id == activeVoiceId || persona.id == activeRoleId,
                    onTap = { vm.onPersonaTap(persona) },
                    onLongPress = { editorMode = EditorMode.Edit(persona) },
                )
            }
            // Footer instruction — quietly tells the user about both
            // interaction modes without crowding the rows.
            item("footer") {
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "Tap to apply. Tap the active role again to clear it. " +
                        "Long-press a custom one to edit.",
                    style = typography.sansSmall.copy(color = colors.inkSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    // Editor dialog — opened by a section header's "+ New" or by long-
    // pressing a custom row. New persona kind comes from which section
    // header was tapped; edits inherit the persona's existing kind.
    when (val mode = editorMode) {
        is EditorMode.New -> PersonaEditorDialog(
            initial = null,
            kindForCreate = mode.kind,
            onDismiss = { editorMode = null },
            onSave = { name, tagline, text ->
                editorMode = null
                vm.addCustom(name, tagline, text, mode.kind)
            },
            onDelete = null,
        )
        is EditorMode.Edit -> PersonaEditorDialog(
            initial = mode.persona,
            kindForCreate = null,
            onDismiss = { editorMode = null },
            onSave = { name, tagline, text ->
                editorMode = null
                vm.updateCustom(mode.persona.id, name, tagline, text)
            },
            onDelete = {
                editorMode = null
                vm.deleteCustom(mode.persona.id)
            },
        )
        null -> Unit
    }
}

/** Editor dialog state — null when closed. */
private sealed interface EditorMode {
    /** Creating a new custom persona in the given slot. */
    data class New(val kind: PersonaKind) : EditorMode
    /** Editing an existing custom persona (kind is fixed at edit-time). */
    data class Edit(val persona: Persona) : EditorMode
}

/**
 * Section header with a label on the left and a "+ New" affordance on
 * the right. Used for VOICES and ROLES — tapping "+ New" opens the
 * editor dialog with the section's kind pre-selected, so the new
 * persona lands in the right slot without an extra picker step.
 */
@Composable
private fun SectionHeader(label: String, onAdd: () -> Unit) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.uppercase(),
            style = typography.sansLabel.copy(color = colors.inkSubtle),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "+ New",
            style = typography.sansLabel.copy(
                color = colors.ink,
                fontWeight = FontWeight.SemiBold,
            ),
            modifier = Modifier
                .clip(shapes.xsmall)
                .clickable(onClick = onAdd)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/**
 * One persona row. Letter-avatar tile + name/tagline column + status
 * pill. Active row gets a 2dp ink border; non-active rows are
 * borderless on the page background.
 *
 * Long-press support is wired via [pointerInput] + [detectTapGestures]
 * — the same gesture handler also serves the tap path so we don't
 * conflict with `Modifier.clickable`. Built-ins pass null for
 * [onLongPress] so the row only responds to tap.
 */
@Composable
private fun PersonaCard(
    persona: Persona,
    active: Boolean,
    onTap: () -> Unit,
    onLongPress: (() -> Unit)?,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(shapes.medium)
            .border(
                width = if (active) 2.dp else 0.dp,
                color = if (active) colors.ink else Color.Transparent,
                shape = shapes.medium,
            )
            .background(colors.background)
            .pointerInput(persona.id, onLongPress) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = onLongPress?.let { handler -> { handler() } },
                )
            }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LetterAvatar(letter = persona.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?")
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = persona.name,
                style = typography.serifBody.copy(
                    color = colors.ink,
                    fontSize = 18.sp,
                ),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = persona.tagline,
                style = typography.serifBody.copy(
                    color = colors.inkMuted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
            )
        }
        Spacer(Modifier.size(8.dp))
        // Pill differentiates voice / role / custom so the user can tell
        // at a glance which axis each item is on. Active state filled,
        // others muted — same color logic as before.
        StatusPill(
            text = if (active) "ACTIVE" else persona.kind.pillLabel,
            filled = active,
        )
    }
}

/**
 * Trailing pill on each row — "ACTIVE" / "BUILT-IN" / "CUSTOM".
 * ACTIVE is filled ink; the others are subdued surfaceMuted.
 */
@Composable
private fun StatusPill(text: String, filled: Boolean) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Box(
        modifier = Modifier
            .clip(shapes.xsmall)
            .background(if (filled) colors.ink else colors.surfaceMuted)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = text,
            style = typography.sansLabel.copy(
                color = if (filled) colors.background else colors.inkSubtle,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

/**
 * 56dp square letter tile — surfaceMuted fill, serif letter centered.
 * Pulls double duty: lends each persona a tiny "logo," and reinforces
 * the document/print typography by promoting a letter to its own glyph.
 */
@Composable
private fun LetterAvatar(letter: String) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(shapes.small)
            .background(colors.surfaceMuted),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            style = typography.serifBody.copy(
                color = colors.ink,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

/**
 * Combined add + edit dialog. [initial] = null means create mode (no
 * Delete affordance, primary button reads "Create"); non-null means
 * edit mode (fields prefilled, Delete shown alongside Cancel).
 *
 * [kindForCreate] is the slot the new persona will fill in create mode;
 * drives the dialog title ("New voice" / "New role") so the user knows
 * which section they're adding to. Ignored when editing.
 *
 * Validation: name and instructions must be non-blank to save.
 * Delete confirms with a follow-up dialog — losing a persona is
 * destructive enough to warrant the extra tap.
 */
@Composable
private fun PersonaEditorDialog(
    initial: Persona?,
    kindForCreate: PersonaKind?,
    onDismiss: () -> Unit,
    onSave: (name: String, tagline: String, text: String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val colors = UndercurrentTheme.colors

    var name by remember { mutableStateOf(initial?.name ?: "") }
    var tagline by remember { mutableStateOf(initial?.tagline ?: "") }
    var text by remember { mutableStateOf(initial?.systemPromptText ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }

    val isEdit = initial != null
    val title = when {
        isEdit -> "Edit persona"
        kindForCreate == PersonaKind.Role -> "New role"
        else -> "New voice"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LabeledField(
                    label = "Name",
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Tech writer",
                )
                LabeledField(
                    label = "Tagline",
                    value = tagline,
                    onValueChange = { tagline = it },
                    placeholder = "Optional one-liner shown in the picker.",
                )
                LabeledField(
                    label = "Instructions",
                    value = text,
                    onValueChange = { text = it },
                    placeholder = "Speak as a senior tech writer. Use active voice. " +
                        "Avoid jargon unless defined.",
                    minLines = 4,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && text.isNotBlank(),
                onClick = { onSave(name.trim(), tagline.trim(), text.trim()) },
            ) { Text(if (isEdit) "Save" else "Create") }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Delete sits on the left of the dismiss row when in edit
                // mode — separated from the primary Save so a thumb-slip
                // doesn't wipe the persona by accident.
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
            text = { Text("This custom persona will be permanently removed.") },
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
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Column {
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
                textStyle = typography.serifBody.copy(color = colors.ink),
                cursorBrush = SolidColor(colors.accent),
                minLines = minLines,
            )
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = typography.serifBody.copy(color = colors.inkSubtle),
                )
            }
        }
    }
}

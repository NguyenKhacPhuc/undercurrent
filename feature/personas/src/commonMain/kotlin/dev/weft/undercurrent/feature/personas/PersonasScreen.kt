package dev.weft.undercurrent.feature.personas

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
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.core.model.BuiltInPersonas
import dev.weft.undercurrent.core.model.Persona
import dev.weft.undercurrent.core.model.PersonaKind
import dev.weft.undercurrent.core.ui.ScreenScaffold
import org.koin.compose.viewmodel.koinViewModel

/**
 * Persona picker — the user picks one "voice" + optional "role" that
 * inject per-turn instructions into the agent's system prompt. Two
 * sections (Voices, Roles); built-ins plus user-created customs in
 * each; a "+ New" affordance per section.
 *
 * KMP — commonMain. Moved from
 * `app/.../features/personas/PersonasScreen.kt`. Adjustments:
 *   - Imports from `:core:ui` / `:core:design-system` / `:core:model`.
 *   - `onStartCreator` parameter now takes a [PersonaKind] (was a
 *     `CreatorKind` enum from the not-yet-migrated `:feature:creator`).
 *     The host's nav glue translates the kind to whatever the creator
 *     screen consumes.
 *   - `org.koin.compose.viewmodel.koinViewModel`.
 */
@Composable
fun PersonasScreen(
    onBack: () -> Unit,
    onStartCreator: (PersonaKind) -> Unit = {},
    store: PersonasViewModel = koinViewModel(),
) {
    val s by store.state.collectAsState()
    val activeVoice = s.activeVoice
    val activeRole = s.activeRole
    val customPersonas = s.customPersonas

    val activeVoiceId = activeVoice.id
    val activeRoleId = activeRole?.id

    var editorMode by remember { mutableStateOf<EditorMode?>(null) }

    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography

    val customVoices = remember(customPersonas) {
        customPersonas.filter { it.kind != PersonaKind.Role }
    }
    val customRoles = remember(customPersonas) {
        customPersonas.filter { it.kind == PersonaKind.Role }
    }

    ScreenScaffold(
        title = "Personas",
        onBack = onBack,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().weight(1f),
        ) {
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
            item("voices-label") {
                SectionHeader(
                    label = "Voices",
                    onAdd = { onStartCreator(PersonaKind.Voice) },
                )
            }
            items(BuiltInPersonas.Voices, key = { it.id }) { persona ->
                PersonaCard(
                    persona = persona,
                    active = persona.id == activeVoiceId || persona.id == activeRoleId,
                    onTap = { store.dispatch(PersonasIntent.TapPersona(persona)) },
                    onLongPress = null,
                )
            }
            items(customVoices, key = { it.id }) { persona ->
                PersonaCard(
                    persona = persona,
                    active = persona.id == activeVoiceId || persona.id == activeRoleId,
                    onTap = { store.dispatch(PersonasIntent.TapPersona(persona)) },
                    onLongPress = { editorMode = EditorMode.Edit(persona) },
                )
            }
            item("roles-label") {
                Spacer(Modifier.height(24.dp))
                SectionHeader(
                    label = "Roles",
                    onAdd = { onStartCreator(PersonaKind.Role) },
                )
            }
            items(BuiltInPersonas.Roles, key = { it.id }) { persona ->
                PersonaCard(
                    persona = persona,
                    active = persona.id == activeVoiceId || persona.id == activeRoleId,
                    onTap = { store.dispatch(PersonasIntent.TapPersona(persona)) },
                    onLongPress = null,
                )
            }
            items(customRoles, key = { it.id }) { persona ->
                PersonaCard(
                    persona = persona,
                    active = persona.id == activeVoiceId || persona.id == activeRoleId,
                    onTap = { store.dispatch(PersonasIntent.TapPersona(persona)) },
                    onLongPress = { editorMode = EditorMode.Edit(persona) },
                )
            }
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

    when (val mode = editorMode) {
        is EditorMode.New -> PersonaEditorDialog(
            initial = null,
            kindForCreate = mode.kind,
            onDismiss = { editorMode = null },
            onSave = { name, tagline, text ->
                editorMode = null
                store.dispatch(PersonasIntent.AddCustom(name, tagline, text, mode.kind))
            },
            onDelete = null,
        )
        is EditorMode.Edit -> PersonaEditorDialog(
            initial = mode.persona,
            kindForCreate = null,
            onDismiss = { editorMode = null },
            onSave = { name, tagline, text ->
                editorMode = null
                store.dispatch(PersonasIntent.UpdateCustom(mode.persona.id, name, tagline, text))
            },
            onDelete = {
                editorMode = null
                store.dispatch(PersonasIntent.DeleteCustom(mode.persona.id))
            },
        )
        null -> Unit
    }
}

private sealed interface EditorMode {
    data class New(val kind: PersonaKind) : EditorMode
    data class Edit(val persona: Persona) : EditorMode
}

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
        StatusPill(
            text = if (active) "ACTIVE" else persona.kind.pillLabel,
            filled = active,
        )
    }
}

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

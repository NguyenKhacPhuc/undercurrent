package dev.weft.undercurrent.features.keypaste

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.contracts.ProviderKind
import dev.weft.undercurrent.features.providers.ValidationResult
import dev.weft.undercurrent.features.providers.apiConsoleUrl
import dev.weft.undercurrent.features.providers.signupHint
import dev.weft.undercurrent.features.providers.validateKey
import dev.weft.undercurrent.theme.UndercurrentTheme
import dev.weft.undercurrent.ui.openInBrowser
import kotlinx.coroutines.launch

/**
 * Key paste flow. Provider-aware — the title, body, signup hint,
 * placeholder, and validation target all reflect the active
 * [ProviderKind] (chosen during onboarding or in Settings).
 *
 * Voice: leads with the privacy framing ("Your key. Your bill. Nothing
 * in between.") rather than apologizing for the BYOK friction. The
 * primary action is "Get an API key from <Provider>" — a Chrome Custom
 * Tab deep link into the provider's console — for users who don't have
 * one yet. Users who already have a key paste below.
 *
 * On Connect: validate against the provider with a 1-token ping; on
 * success, hand the key to [saveKey] (which persists under the active
 * provider's vault alias) and notify the host via [onKeyAccepted].
 */
@Composable
public fun KeyPasteScreen(
    provider: ProviderKind,
    onKeyAccepted: (String) -> Unit,
    saveKey: suspend (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes

    var key by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<Status>(Status.Idle) }

    // CCT toolbar tint follows the active surface so the in-app browser
    // doesn't jarringly switch palettes when the user taps "Get a key".
    val toolbarColorArgb = colors.surface.toArgb()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 480.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            // ─── Privacy framing (the headline pitch) ─────────────────
            Text(
                text = "Connect your key.",
                style = typography.serifBody.copy(
                    color = colors.ink,
                    fontSize = 38.sp,
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.Normal,
                ),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Your key. Your bill. Nothing in between.",
                style = typography.serifBody.copy(
                    color = colors.ink,
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    fontStyle = FontStyle.Italic,
                ),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Undercurrent talks to ${provider.displayName()} from this " +
                    "phone. We never see your conversations or your billing.",
                style = typography.serifBody.copy(
                    color = colors.inkMuted,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                ),
            )

            Spacer(Modifier.height(32.dp))

            // ─── Primary action: Get an API key (CCT deep link) ──────
            // Most users land here without a key — make grabbing one the
            // visually dominant affordance. Pasting comes second.
            PrimaryButton(
                label = "Get an API key from ${provider.displayName()} →",
                onClick = {
                    openInBrowser(context, provider.apiConsoleUrl(), toolbarColorArgb)
                },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = provider.signupHint(),
                style = typography.sansSmall.copy(color = colors.inkSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))

            // ─── OR divider ───────────────────────────────────────────
            OrDivider()

            Spacer(Modifier.height(20.dp))

            // ─── Paste field (for users who already have a key) ───────
            Text(
                text = "PASTE YOUR KEY",
                style = typography.sansLabel.copy(color = colors.inkSubtle),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.medium)
                    .border(width = 1.dp, color = colors.divider, shape = shapes.medium)
                    .background(colors.surface)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = key,
                        onValueChange = { key = it.trim() },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = typography.mono.copy(color = colors.ink),
                        cursorBrush = SolidColor(colors.accent),
                        singleLine = true,
                        enabled = status !is Status.Validating,
                        visualTransformation = if (keyVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    )
                    if (key.isEmpty()) {
                        Text(
                            text = provider.keyPlaceholder(),
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

            Spacer(Modifier.height(16.dp))

            // Connect button — secondary visual weight (border-only) so
            // the "Get a key" primary CTA above remains the eye-pull.
            val enabled = key.isNotBlank() && status !is Status.Validating
            SecondaryButton(
                label = if (status is Status.Validating) "Checking…" else "Connect",
                enabled = enabled,
                onClick = {
                    status = Status.Validating
                    scope.launch {
                        val result = validateKey(provider, key)
                        status = when (result) {
                            is ValidationResult.Ok -> Status.Validated
                            is ValidationResult.Invalid -> Status.Failed(result.message)
                        }
                    }
                },
            )

            when (val s = status) {
                is Status.Failed -> {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = s.message,
                        style = typography.sansSmall.copy(color = colors.error),
                    )
                }
                else -> Unit
            }

            // Storage reassurance footer — quiet, factual.
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Stored encrypted on this device. Only sent to " +
                    "${provider.hostName()} when you message the assistant.",
                style = typography.sansSmall.copy(color = colors.inkSubtle),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }

    LaunchedEffect(status) {
        if (status is Status.Validated) {
            saveKey(key)
            onKeyAccepted(key)
        }
    }
}

/** Full-width ink-filled CTA. The "Get a key" primary action. */
@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .background(colors.ink)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = typography.sansHeader.copy(
                color = colors.background,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

/** Full-width border-only CTA. The "Connect" action. */
@Composable
private fun SecondaryButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    val borderColor = if (enabled) colors.ink else colors.divider
    val textColor = if (enabled) colors.ink else colors.inkSubtle
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .border(width = 1.5.dp, color = borderColor, shape = shapes.medium)
            .background(colors.background)
            .let { if (enabled) it.clickable(onClick = onClick) else it }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = typography.sansHeader.copy(
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

/** Thin horizontal rule with a centered "OR" label. Separates Get-a-key
 *  from Paste-a-key visually so it's clear they're two ways into the
 *  same outcome, not a sequence. */
@Composable
private fun OrDivider() {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(colors.divider),
        )
        Text(
            text = "OR",
            style = typography.sansLabel.copy(color = colors.inkSubtle),
            modifier = Modifier.padding(horizontal = 14.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(colors.divider),
        )
    }
}

private fun ProviderKind.displayName(): String = when (this) {
    ProviderKind.Anthropic -> "Anthropic"
    ProviderKind.OpenAI -> "OpenAI"
    ProviderKind.OpenRouter -> "OpenRouter"
    ProviderKind.DeepSeek -> "DeepSeek"
}

private fun ProviderKind.hostName(): String = when (this) {
    ProviderKind.Anthropic -> "api.anthropic.com"
    ProviderKind.OpenAI -> "api.openai.com"
    ProviderKind.OpenRouter -> "openrouter.ai"
    ProviderKind.DeepSeek -> "api.deepseek.com"
}

private fun ProviderKind.keyPlaceholder(): String = when (this) {
    ProviderKind.Anthropic -> "sk-ant-…"
    ProviderKind.OpenAI -> "sk-…"
    ProviderKind.OpenRouter -> "sk-or-…"
    ProviderKind.DeepSeek -> "sk-…"
}

private sealed class Status {
    data object Idle : Status()
    data object Validating : Status()
    data object Validated : Status()
    data class Failed(val message: String) : Status()
}

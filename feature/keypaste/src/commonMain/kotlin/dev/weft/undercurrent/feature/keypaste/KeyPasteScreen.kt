package dev.weft.undercurrent.feature.keypaste

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.core.model.ProviderKind
import dev.weft.undercurrent.core.model.apiConsoleUrl
import dev.weft.undercurrent.core.model.hostName
import dev.weft.undercurrent.core.model.keyPlaceholder
import dev.weft.undercurrent.core.model.signupHint
import dev.weft.undercurrent.core.resources.Res
import dev.weft.undercurrent.core.resources.keypaste_body_format
import dev.weft.undercurrent.core.resources.keypaste_checking
import dev.weft.undercurrent.core.resources.keypaste_connect
import dev.weft.undercurrent.core.resources.keypaste_footer_format
import dev.weft.undercurrent.core.resources.keypaste_get_key_format
import dev.weft.undercurrent.core.resources.keypaste_hide
import dev.weft.undercurrent.core.resources.keypaste_or
import dev.weft.undercurrent.core.resources.keypaste_paste_label
import dev.weft.undercurrent.core.resources.keypaste_show
import dev.weft.undercurrent.core.resources.keypaste_subtitle
import dev.weft.undercurrent.core.resources.keypaste_title
import dev.weft.undercurrent.core.domain.KeyValidationRepository
import dev.weft.undercurrent.core.domain.ValidationResult
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun KeyPasteScreen(
    provider: ProviderKind,
    validator: KeyValidationRepository,
    onKeyAccepted: (String) -> Unit,
    saveKey: suspend (String) -> Unit,
    onOpenConsole: (url: String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes

    var key by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<Status>(Status.Idle) }

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
            Text(
                text = stringResource(Res.string.keypaste_title),
                style = typography.serifBody.copy(
                    color = colors.ink,
                    fontSize = 38.sp,
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.Normal,
                ),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(Res.string.keypaste_subtitle),
                style = typography.serifBody.copy(
                    color = colors.ink,
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    fontStyle = FontStyle.Italic,
                ),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(Res.string.keypaste_body_format, provider.displayName),
                style = typography.serifBody.copy(
                    color = colors.inkMuted,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                ),
            )

            Spacer(Modifier.height(32.dp))

            PrimaryButton(
                label = stringResource(Res.string.keypaste_get_key_format, provider.displayName),
                onClick = { onOpenConsole(provider.apiConsoleUrl()) },
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
            OrDivider()
            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(Res.string.keypaste_paste_label),
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
                Text(
                    text = if (keyVisible) stringResource(Res.string.keypaste_hide) else stringResource(Res.string.keypaste_show),
                    style = typography.sansLabel.copy(color = colors.inkMuted),
                    modifier = Modifier
                        .clickable { keyVisible = !keyVisible }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            val enabled = key.isNotBlank() && status !is Status.Validating
            SecondaryButton(
                label = if (status is Status.Validating) stringResource(Res.string.keypaste_checking) else stringResource(Res.string.keypaste_connect),
                enabled = enabled,
                onClick = {
                    status = Status.Validating
                    scope.launch {
                        val result = validator.validateKey(provider, key)
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

            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(Res.string.keypaste_footer_format, provider.hostName()),
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
            text = stringResource(Res.string.keypaste_or),
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

private sealed class Status {
    data object Idle : Status()
    data object Validating : Status()
    data object Validated : Status()
    data class Failed(val message: String) : Status()
}

@Preview
@Composable
private fun KeyPasteScreenPreview() {
    UndercurrentTheme {
        KeyPasteScreen(
            provider = ProviderKind.Anthropic,
            validator = object : KeyValidationRepository {
                override suspend fun validateKey(
                    provider: ProviderKind,
                    apiKey: String,
                ): ValidationResult = ValidationResult.Ok
            },
            onKeyAccepted = {},
            saveKey = {},
            onOpenConsole = {},
        )
    }
}

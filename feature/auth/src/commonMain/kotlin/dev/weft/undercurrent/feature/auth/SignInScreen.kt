package dev.weft.undercurrent.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.core.resources.Res
import dev.weft.undercurrent.core.resources.auth_cta_register
import dev.weft.undercurrent.core.resources.auth_cta_sign_in
import dev.weft.undercurrent.core.resources.auth_error_invalid_credentials
import dev.weft.undercurrent.core.resources.auth_error_network
import dev.weft.undercurrent.core.resources.auth_error_rate_limited
import dev.weft.undercurrent.core.resources.auth_field_display_name
import dev.weft.undercurrent.core.resources.auth_field_email
import dev.weft.undercurrent.core.resources.auth_field_password
import dev.weft.undercurrent.core.resources.auth_mode_register
import dev.weft.undercurrent.core.resources.auth_mode_sign_in
import dev.weft.undercurrent.core.resources.auth_register_subtitle
import dev.weft.undercurrent.core.resources.auth_register_title
import dev.weft.undercurrent.core.resources.auth_sign_in_subtitle
import dev.weft.undercurrent.core.resources.auth_sign_in_title
import dev.weft.undercurrent.core.resources.auth_switch_to_sign_in_with_email
import dev.weft.undercurrent.core.resources.common_retry
import dev.weft.undercurrent.core.ui.LabeledField
import dev.weft.undercurrent.core.ui.LoadingOverlay
import dev.weft.undercurrent.core.ui.PrimaryButton
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SignInScreen(
    state: SignInState,
    loading: Boolean = false,
    onIntent: (SignInIntent) -> Unit = {},
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val snackbarHostState = remember { SnackbarHostState() }

    val invalidCredentialsMsg = stringResource(Res.string.auth_error_invalid_credentials)
    val rateLimitedMsg = stringResource(Res.string.auth_error_rate_limited)
    val networkMsg = stringResource(Res.string.auth_error_network)
    val retryLabel = stringResource(Res.string.common_retry)

    LaunchedEffect(state.topError) {
        val err = state.topError ?: return@LaunchedEffect
        val message = when (err) {
            TopError.InvalidCredentials -> invalidCredentialsMsg
            TopError.RateLimited -> rateLimitedMsg
            TopError.Network -> networkMsg
            is TopError.Message -> err.message
        }
        val action = if (err is TopError.Network) retryLabel else null
        val result = snackbarHostState.showSnackbar(message = message, actionLabel = action)
        if (result == SnackbarResult.ActionPerformed || result == SnackbarResult.Dismissed) {
            onIntent(SignInIntent.ClearTopError)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 32.dp),
        ) {
            Text(
                text = stringResource(
                    if (state.mode == SignInState.Mode.SignIn) Res.string.auth_sign_in_title
                    else Res.string.auth_register_title,
                ),
                style = typography.sansHeader.copy(color = colors.ink),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    when (state.mode) {
                        SignInState.Mode.SignIn -> Res.string.auth_sign_in_subtitle
                        SignInState.Mode.Register -> Res.string.auth_register_subtitle
                    },
                ),
                style = typography.serifBody.copy(color = colors.inkSubtle),
            )
            Spacer(Modifier.height(24.dp))

            ModeToggle(
                mode = state.mode,
                onSwitch = { onIntent(SignInIntent.SwitchMode) },
            )
            Spacer(Modifier.height(24.dp))

            if (state.mode == SignInState.Mode.Register) {
                LabeledField(
                    label = stringResource(Res.string.auth_field_display_name),
                    value = state.displayName,
                    onValueChange = { onIntent(SignInIntent.DisplayNameChanged(it)) },
                    fieldError = state.fieldErrors["displayName"],
                )
                Spacer(Modifier.height(16.dp))
            }
            LabeledField(
                label = stringResource(Res.string.auth_field_email),
                value = state.email,
                onValueChange = { onIntent(SignInIntent.EmailChanged(it)) },
                keyboardType = KeyboardType.Email,
                fieldError = state.fieldErrors["email"],
            )
            Spacer(Modifier.height(16.dp))
            LabeledField(
                label = stringResource(Res.string.auth_field_password),
                value = state.password,
                onValueChange = { onIntent(SignInIntent.PasswordChanged(it)) },
                keyboardType = KeyboardType.Password,
                visualTransformation = PasswordVisualTransformation(),
                fieldError = state.fieldErrors["password"],
            )
            Spacer(Modifier.height(24.dp))

            if (state.showSwitchToSignInShortcut) {
                Text(
                    text = stringResource(Res.string.auth_switch_to_sign_in_with_email),
                    style = typography.sansLabel.copy(color = colors.accent),
                    modifier = Modifier
                        .clickable { onIntent(SignInIntent.SwitchToSignInWithEmail) }
                        .padding(vertical = 4.dp),
                )
                Spacer(Modifier.height(16.dp))
            }

            PrimaryButton(
                label = stringResource(
                    if (state.mode == SignInState.Mode.SignIn) Res.string.auth_cta_sign_in
                    else Res.string.auth_cta_register,
                ),
                enabled = state.canSubmit,
                onClick = { onIntent(SignInIntent.Continue) },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (loading) {
            LoadingOverlay()
        }
    }
}

@Composable
private fun ModeToggle(
    mode: SignInState.Mode,
    onSwitch: () -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .border(1.dp, colors.divider, shapes.medium)
            .background(colors.surface),
    ) {
        SignInState.Mode.entries.forEach { entry ->
            val selected = entry == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (selected) colors.background else colors.surface)
                    .clickable(enabled = !selected) { onSwitch() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(
                        if (entry == SignInState.Mode.SignIn) Res.string.auth_mode_sign_in
                        else Res.string.auth_mode_register,
                    ),
                    style = typography.sansLabel.copy(
                        color = if (selected) colors.ink else colors.inkMuted,
                    ),
                )
            }
        }
    }
}

@Preview
@Composable
private fun SignInScreenPreviewSignIn() {
    UndercurrentTheme {
        SignInScreen(
            state = SignInState(
                mode = SignInState.Mode.SignIn,
                email = "phuc@example.com",
                password = "hunter2-correct",
            ),
        )
    }
}

@Preview
@Composable
private fun SignInScreenPreviewRegisterWithErrors() {
    UndercurrentTheme {
        SignInScreen(
            state = SignInState(
                mode = SignInState.Mode.Register,
                displayName = "Phuc",
                email = "not-an-email",
                password = "short",
                fieldErrors = mapOf(
                    "email" to "must be a valid email address",
                    "password" to "must be at least 8 characters",
                ),
            ),
        )
    }
}

@Preview
@Composable
private fun SignInScreenPreviewSwitchShortcut() {
    UndercurrentTheme {
        SignInScreen(
            state = SignInState(
                mode = SignInState.Mode.Register,
                displayName = "Phuc",
                email = "taken@example.com",
                password = "hunter2-correct",
                topError = TopError.Message("An account with this email already exists"),
                showSwitchToSignInShortcut = true,
            ),
        )
    }
}

@Preview
@Composable
private fun SignInScreenPreviewLoading() {
    UndercurrentTheme {
        SignInScreen(
            state = SignInState(
                email = "phuc@example.com",
                password = "hunter2-correct",
            ),
            loading = true,
        )
    }
}

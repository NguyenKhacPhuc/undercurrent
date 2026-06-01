package dev.weft.undercurrent.feature.signin

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Stateless sign-in / register form. State in, callbacks out — no
 * Koin lookups, no flow collection. Routes the user through:
 *
 *  - Mode toggle (Sign In / Register) at the top
 *  - Email + password fields (always)
 *  - Display-name field (Register only)
 *  - Top-error region above the form (per-error variant)
 *  - Per-field error region under each field (Register 400-with-details)
 *  - Continue button (disabled until [SignInState.canSubmit])
 *  - "Switch to Sign In with this email" shortcut when surfaced
 *    by a Register 409 (`email_already_registered`)
 *
 * Wrapped by [SignInRoute] which injects [SignInViewModel] and turns
 * [SignInEffect.SignedIn] into a host-level resume.
 */
@Composable
fun SignInScreen(
    state: SignInState,
    onIntent: (SignInIntent) -> Unit = {},
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp, bottom = 32.dp),
    ) {
        Text(
            text = if (state.mode == SignInState.Mode.SignIn) "Welcome back" else "Create your account",
            style = typography.sansHeader.copy(color = colors.ink),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = when (state.mode) {
                SignInState.Mode.SignIn -> "Sign in with your email and password."
                SignInState.Mode.Register -> "Pick a display name, email, and password."
            },
            style = typography.serifBody.copy(color = colors.inkSubtle),
        )
        Spacer(Modifier.height(24.dp))

        ModeToggle(
            mode = state.mode,
            enabled = !state.submitting,
            onSwitch = { onIntent(SignInIntent.SwitchMode) },
        )
        Spacer(Modifier.height(24.dp))

        state.topError?.let { err ->
            TopErrorBanner(
                error = err,
                onDismiss = { onIntent(SignInIntent.ClearTopError) },
            )
            Spacer(Modifier.height(16.dp))
        }

        if (state.mode == SignInState.Mode.Register) {
            LabeledField(
                label = "Display name",
                value = state.displayName,
                onValueChange = { onIntent(SignInIntent.DisplayNameChanged(it)) },
                enabled = !state.submitting,
                fieldError = state.fieldErrors["displayName"],
            )
            Spacer(Modifier.height(16.dp))
        }
        LabeledField(
            label = "Email",
            value = state.email,
            onValueChange = { onIntent(SignInIntent.EmailChanged(it)) },
            enabled = !state.submitting,
            keyboardType = KeyboardType.Email,
            fieldError = state.fieldErrors["email"],
        )
        Spacer(Modifier.height(16.dp))
        LabeledField(
            label = "Password",
            value = state.password,
            onValueChange = { onIntent(SignInIntent.PasswordChanged(it)) },
            enabled = !state.submitting,
            keyboardType = KeyboardType.Password,
            visualTransformation = PasswordVisualTransformation(),
            fieldError = state.fieldErrors["password"],
        )
        Spacer(Modifier.height(24.dp))

        if (state.showSwitchToSignInShortcut) {
            Text(
                text = "Switch to Sign In with this email",
                style = typography.sansLabel.copy(color = colors.accent),
                modifier = Modifier
                    .clickable(enabled = !state.submitting) {
                        onIntent(SignInIntent.SwitchToSignInWithEmail)
                    }
                    .padding(vertical = 4.dp),
            )
            Spacer(Modifier.height(16.dp))
        }

        ContinueButton(
            label = when {
                state.submitting -> "…"
                state.mode == SignInState.Mode.SignIn -> "Sign In"
                else -> "Create account"
            },
            enabled = state.canSubmit,
            onClick = { onIntent(SignInIntent.Continue) },
        )
    }
}

@Composable
private fun ModeToggle(
    mode: SignInState.Mode,
    enabled: Boolean,
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
                    .clickable(enabled = enabled && !selected) { onSwitch() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (entry == SignInState.Mode.SignIn) "Sign In" else "Register",
                    style = typography.sansLabel.copy(
                        color = if (selected) colors.ink else colors.inkMuted,
                    ),
                )
            }
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    fieldError: String? = null,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Column {
        Text(
            text = label,
            style = typography.sansLabel.copy(color = colors.inkSubtle),
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shapes.medium)
                .border(
                    width = 1.dp,
                    color = if (fieldError != null) colors.error else colors.divider,
                    shape = shapes.medium,
                )
                .background(colors.surface)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = typography.serifBody.copy(color = colors.ink),
                cursorBrush = SolidColor(colors.accent),
                singleLine = true,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                visualTransformation = visualTransformation,
            )
        }
        fieldError?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                text = it,
                style = typography.sansLabel.copy(color = colors.error),
            )
        }
    }
}

@Composable
private fun TopErrorBanner(error: TopError, onDismiss: () -> Unit) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .background(colors.surfaceMuted)
            .padding(16.dp),
    ) {
        Text(
            text = when (error) {
                TopError.InvalidCredentials -> "Invalid email or password."
                TopError.RateLimited -> "Too many failed attempts. Try again later."
                TopError.Network -> "Couldn't reach the server. Check your connection."
                is TopError.Message -> error.message
            },
            style = typography.serifBody.copy(color = colors.error),
        )
        if (error is TopError.Network) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Retry",
                style = typography.sansLabel.copy(color = colors.error),
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun ContinueButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .background(if (enabled) colors.accent else colors.divider)
            .clickable(enabled = enabled) { onClick() }
            .alpha(if (enabled) 1f else 0.6f)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = typography.sansLabel.copy(color = colors.surface),
            textAlign = TextAlign.Center,
        )
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
                topError = TopError.Message("One or more fields are invalid"),
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
private fun SignInScreenPreviewNetworkError() {
    UndercurrentTheme {
        SignInScreen(
            state = SignInState(
                email = "phuc@example.com",
                password = "hunter2-correct",
                topError = TopError.Network,
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

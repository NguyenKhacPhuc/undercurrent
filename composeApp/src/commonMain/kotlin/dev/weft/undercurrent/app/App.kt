package dev.weft.undercurrent.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.core.model.AppEffect
import dev.weft.undercurrent.core.model.ThemeMode

/**
 * Root Composable shared between Android and iOS. Owns the theme
 * wrapper, the snackbar host, the permission-dialog overlay, and the
 * top-level screen switch.
 *
 * Platform-specific bits (chat surface, agent-rendered tree, mini-app
 * tree preview, OS bridges) are injected through [PlatformAdapter].
 *
 * @param store the platform's [AppStore] implementation (Android wires
 *   `WeftAppStore`; iOS wires the stub).
 * @param platform substrate-coupled routes + OS callbacks. iOS passes
 *   placeholder composables for the substrate-only screens.
 */
@Composable
fun App(
    store: AppStore,
    platform: PlatformAdapter,
) {
    val state by store.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val systemDark = isSystemInDarkTheme()
    val darkMode = when (state.themePrefs.mode) {
        ThemeMode.Auto -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    LaunchedEffect(store) {
        store.dispatch(AppIntent.Resume)
    }

    LaunchedEffect(store, snackbarHostState) {
        store.effects.collect { effect ->
            when (effect) {
                is AppEffect.Error -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    UndercurrentTheme(palette = state.themePrefs.palette, darkMode = darkMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                ScreenRouter(
                    state = state,
                    store = store,
                    platform = platform,
                )

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )

                state.pendingPermissionDialog?.let { pending ->
                    PermissionNeededDialog(
                        state = pending,
                        onOpenSettings = {
                            platform.onOpenAppDetailsSettings()
                            store.dispatch(AppIntent.DismissPermissionDialog)
                        },
                        onDismiss = {
                            store.dispatch(AppIntent.DismissPermissionDialog)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionNeededDialog(
    state: PermissionDialogState,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.friendlyTitle) },
        text = { Text(state.friendlyBody) },
        confirmButton = {
            TextButton(onClick = onOpenSettings) { Text("Open Settings") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        },
    )
}

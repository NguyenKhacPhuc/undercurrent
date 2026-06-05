package dev.weft.undercurrent.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
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
import dev.weft.undercurrent.core.model.PermissionDialogState
import dev.weft.undercurrent.core.model.ThemeMode
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.core.resources.Res
import dev.weft.undercurrent.core.resources.permission_dialog_not_now
import dev.weft.undercurrent.core.resources.permission_dialog_open_settings
import dev.weft.undercurrent.core.ui.MiniAppConsentSheet
import dev.weft.undercurrent.feature.miniapps.MiniAppIntent
import dev.weft.undercurrent.feature.miniapps.MiniAppViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun App(
    store: AppViewModel,
    platform: PlatformAdapter,
    overlays: @Composable BoxScope.() -> Unit = {},
) {
    val state by store.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val systemDark = isSystemInDarkTheme()
    val darkMode = when (state.themePrefs.mode) {
        ThemeMode.Auto -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    LaunchedEffect(store) { store.resume() }

    LaunchedEffect(store, snackbarHostState) {
        store.effects.collect { effect ->
            when (effect) {
                is AppEffect.Error -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    UndercurrentTheme(palette = state.themePrefs.palette, darkMode = darkMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            // The rendered-tree screen draws edge-to-edge so a full-screen
            // HTML mini-app can use the whole display (under the system bars);
            // every other screen keeps the safe-area inset.
            val edgeToEdge = state.screen is Screen.RenderedTree
            Box(
                modifier = Modifier.fillMaxSize()
                    .then(if (edgeToEdge) Modifier else Modifier.safeDrawingPadding()),
            ) {
                ScreenRouter(platform = platform)

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )

                state.pendingPermissionDialog?.let { pending ->
                    PermissionNeededDialog(
                        state = pending,
                        onOpenSettings = {
                            platform.onOpenAppDetailsSettings()
                            store.dismissPermissionDialog()
                        },
                        onDismiss = { store.dismissPermissionDialog() },
                    )
                }

                state.pendingMiniAppConsent?.let { consent ->
                    val miniAppVm: MiniAppViewModel = koinInject()
                    MiniAppConsentSheet(
                        request = consent,
                        onApprove = { miniAppVm.dispatch(MiniAppIntent.ApproveConsent(consent.miniAppId)) },
                        onDeny = { miniAppVm.dispatch(MiniAppIntent.DenyConsent(consent.miniAppId)) },
                    )
                }

                overlays()
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
            TextButton(onClick = onOpenSettings) { Text(stringResource(Res.string.permission_dialog_open_settings)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.permission_dialog_not_now)) }
        },
    )
}

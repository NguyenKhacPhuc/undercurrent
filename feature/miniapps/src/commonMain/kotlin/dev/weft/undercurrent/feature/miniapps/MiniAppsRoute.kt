package dev.weft.undercurrent.feature.miniapps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.weft.undercurrent.core.model.MiniApp
import org.koin.compose.viewmodel.koinViewModel

/**
 * Stateful entry point for the mini-apps management screen. Injects
 * [MiniAppsViewModel], hoists its state, and turns edits into intents.
 * Platform-specific bits (the [treePreview] renderer + navigation /
 * open callbacks) come in as parameters so the host wires them.
 */
@Composable
fun MiniAppsRoute(
    treePreview: @Composable (treeJson: String, onTap: () -> Unit) -> Unit,
    onBack: () -> Unit,
    onOpenMiniApp: (MiniApp) -> Unit,
    onStartCreator: () -> Unit = {},
) {
    val vm: MiniAppsViewModel = koinViewModel()
    val state by vm.state.collectAsState()
    MiniAppsScreen(
        state = state,
        treePreview = treePreview,
        onBack = onBack,
        onOpenMiniApp = onOpenMiniApp,
        onStartCreator = onStartCreator,
        onUpdate = { id, name, emoji, prompt ->
            vm.dispatch(MiniAppsIntent.Update(id, name, emoji, prompt))
        },
        onDelete = { vm.dispatch(MiniAppsIntent.Delete(it)) },
        onSetApprovedScopes = { id, scopes ->
            vm.dispatch(MiniAppsIntent.SetApprovedScopes(id, scopes))
        },
    )
}

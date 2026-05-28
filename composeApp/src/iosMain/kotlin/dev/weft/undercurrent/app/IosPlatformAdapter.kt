package dev.weft.undercurrent.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * iOS [PlatformAdapter] factory — placeholder composables for every
 * substrate-coupled screen and no-op OS bridges. Lets the Compose root
 * render on iOS without any agent runtime.
 *
 * The substrate-coupled routes (Chat / RenderedTree / Creator /
 * MiniApps) render a "coming to iOS" placeholder; the KMP-clean
 * screens (Settings / Personas / Conversations / etc.) work as-is
 * since they consume gateway interfaces that have iOS stubs.
 */
public fun iosPlatformAdapter(): PlatformAdapter = PlatformAdapter(
    chatRoute = { IosPlaceholder(label = "Chat") },
    renderedTreeRoute = { IosPlaceholder(label = "Rendered tree") },
    miniAppsRoute = { IosPlaceholder(label = "Mini apps") },
    creatorRoute = { IosPlaceholder(label = "Creator") },
    onOpenUrl = { /* iOS UIApplication.shared.open — wire later */ },
    onCopyText = { /* iOS UIPasteboard.general.string — wire later */ },
    onRestartProcess = { /* iOS process model differs — N/A */ },
    onOpenAppDetailsSettings = { /* UIApplication.openSettingsURLString — wire later */ },
    onOpenSaveDialog = { /* mini-app save dialog — wire when iOS chat lands */ },
)

@Composable
private fun IosPlaceholder(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$label — coming to iOS")
    }
}

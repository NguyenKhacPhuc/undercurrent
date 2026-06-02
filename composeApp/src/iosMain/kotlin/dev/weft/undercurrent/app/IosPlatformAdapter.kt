package dev.weft.undercurrent.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.Navigator
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.feature.miniapps.MiniAppIntent
import dev.weft.undercurrent.feature.miniapps.MiniAppViewModel
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.compose.koinInject
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

fun iosPlatformAdapter(): PlatformAdapter = PlatformAdapter(
    chatRoute = { IosPlaceholder(label = "Chat") },
    renderedTreeRoute = { IosPlaceholder(label = "Rendered tree") },
    miniAppsRoute = { IosMiniAppsRoute() },
    creatorRoute = { IosPlaceholder(label = "Creator") },
    onOpenUrl = { url -> openUrl(url) },
    onRestartProcess = { },
    onOpenAppDetailsSettings = { openUrl(UIApplicationOpenSettingsURLString) },
)

@Composable
private fun IosMiniAppsRoute() {
    val navigationVm: Navigator = koinInject()
    val miniAppVm: MiniAppViewModel = koinInject()
    dev.weft.undercurrent.feature.miniapps.MiniAppsScreen(
        treePreview = { _, _ ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { Text("(no preview)") }
        },
        onBack = {
            navigationVm.dispatch(NavigationIntent.Navigate(Screen.Chat))
        },
        onOpenMiniApp = { miniApp ->
            miniAppVm.dispatch(
                MiniAppIntent.InvokeMiniApp(
                    miniAppId = miniApp.id,
                    triggerPrompt = miniApp.triggerPrompt,
                    cachedRenderTreeJson = null,
                ),
            )
        },
        onStartCreator = { },
    )
}

@Composable
private fun IosPlaceholder(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$label — coming to iOS")
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    if (UIApplication.sharedApplication.canOpenURL(nsUrl)) {
        UIApplication.sharedApplication.openURL(
            url = nsUrl,
            options = emptyMap<Any?, Any>(),
            completionHandler = null,
        )
    }
}

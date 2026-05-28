package dev.weft.undercurrent.app

import androidx.compose.ui.window.ComposeUIViewController
import org.koin.compose.KoinContext
import platform.UIKit.UIViewController

/**
 * Swift entry point. The `iosApp/` Xcode project calls this from its
 * SwiftUI `@main App` (typically via `UIViewControllerRepresentable`)
 * to mount the Compose Multiplatform UI inside the SwiftUI scene.
 *
 * Pre-condition: [initKoin] must have been called before the first
 * invocation. Swift wires it in `App.init` or via an `applicationDidFinishLaunching`
 * shim.
 *
 * Resolves [AppStore] from Koin so the same singleton is shared with
 * the rest of the iOS Koin module (gateways, repos). [iosPlatformAdapter]
 * is constructed locally — it's stateless.
 */
public fun MainViewController(): UIViewController = ComposeUIViewController {
    KoinContext {
        val store = org.koin.mp.KoinPlatform.getKoin().get<AppStore>()
        App(store = store, platform = iosPlatformAdapter())
    }
}

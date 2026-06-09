package dev.weft.undercurrent.core.ext

import platform.UIKit.UIViewController

/**
 * Cross-language UI bridge slots: Kotlin-declared handles the Swift app
 * conforms to / fills, so a Compose `actual` can embed a native SwiftUI
 * view (see `:core:ui`'s `PlatformActionButton.ios.kt`).
 *
 * These live in `:core:ext` — a tiny leaf module with no `api` deps — so
 * `composeApp` can `export` it into the framework's ObjC header for Swift
 * without dragging the whole UI/Skiko/Koin surface into the ABI.
 */

/**
 * A native, SwiftUI-backed [UIViewController] that Compose drives: built
 * once via the registered factory, then [update]d on each recomposition.
 */
interface NativeActionButton {
    val viewController: UIViewController
    fun update(label: String, enabled: Boolean)
}

/**
 * Slots the Swift app fills at launch (`iosAppApp.swift`). `null` until
 * registered — the Compose `actual` falls back to a pure-Compose
 * rendering, so previews and tests still work.
 */
object NativeViewRegistry {
    var actionButtonFactory: ((onClick: () -> Unit) -> NativeActionButton)? = null
}

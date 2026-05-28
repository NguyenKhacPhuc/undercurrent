package dev.weft.undercurrent.app

import org.koin.core.context.startKoin

/**
 * Bootstrap Koin on iOS. Swift calls this once during
 * `applicationDidFinishLaunching` (or `App.init` if using SwiftUI
 * `@main`) before any [MainViewController] is created.
 *
 * Idempotent at the Swift call site is the caller's responsibility —
 * Koin itself throws on a second `startKoin` so don't call this
 * twice.
 *
 * The Android side has its own bootstrap in
 * `UndercurrentApp.onCreate`; iOS has no `Application` lifecycle so
 * this lives as a top-level function the Xcode side invokes.
 */
public fun initKoin() {
    startKoin {
        modules(iosAppModule)
    }
}

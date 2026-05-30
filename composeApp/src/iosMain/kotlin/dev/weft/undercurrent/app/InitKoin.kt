package dev.weft.undercurrent.app

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.core.context.startKoin

/**
 * Bootstrap Koin + Napier on iOS. Swift calls this once during
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
fun initKoin() {
    // Napier — substrate emits diagnostic traces via Napier; without
    // this base() call they no-op. The iOS DebugAntilog routes through
    // os_log so the lines appear in the Xcode console under the
    // "WeftBindings" subsystem.
    Napier.base(DebugAntilog())
    startKoin {
        modules(iosAllModules)
    }
}

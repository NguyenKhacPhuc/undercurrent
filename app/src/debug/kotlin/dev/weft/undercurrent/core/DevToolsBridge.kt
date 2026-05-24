package dev.weft.undercurrent.core

import androidx.compose.runtime.Composable
import dev.weft.android.WeftRuntime

/**
 * Debug-variant wrapper around [dev.weft.devtools.WeftDevTools].
 *
 * Currently a pass-through — the DevTools FAB has been disabled because
 * it was floating over every screen (including Chat) and getting in the
 * way. The wrapper is kept so re-enabling is a one-line revert:
 *
 *     WeftDevTools(runtime = runtime, enabled = true, content = content)
 *
 * The `debugImplementation("dev.weft.devtools:weft-android-devtools")`
 * dep in `app/build.gradle.kts` is still on the classpath; remove it
 * too if you want a smaller debug APK.
 *
 * Pairs with `src/release/.../DevToolsBridge.kt` (identical body in
 * release). Android Gradle's source-set variant selection picks the
 * right copy at build time.
 */
@Composable
internal fun DevToolsHost(
    @Suppress("UNUSED_PARAMETER") runtime: WeftRuntime,
    content: @Composable () -> Unit,
) {
    content()
}

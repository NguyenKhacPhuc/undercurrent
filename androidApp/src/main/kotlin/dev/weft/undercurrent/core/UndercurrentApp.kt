package dev.weft.undercurrent.core

import android.app.Application
import dev.weft.undercurrent.di.appModule
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Application entry. Boots Koin with [appModule]; everything else
 * (runtime, repos, UI singletons, view-models) is constructed lazily on
 * first `get()` / `koinInject()` / `koinViewModel()`.
 *
 * Registered via `android:name=".core.UndercurrentApp"` in the manifest.
 */
class UndercurrentApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Napier — substrate emits diagnostic traces (WeftBindings etc.)
        // via Napier; without this base() call they no-op. DebugAntilog
        // routes through android.util.Log on Android so the existing
        // `adb logcat -s WeftBindings` workflow keeps working.
        Napier.base(DebugAntilog())
        startKoin {
            androidLogger(Level.INFO)
            androidContext(this@UndercurrentApp)
            modules(appModule)
        }
    }
}

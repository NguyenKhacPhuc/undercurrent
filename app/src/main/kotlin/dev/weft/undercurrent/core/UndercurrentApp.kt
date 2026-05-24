package dev.weft.undercurrent.core

import android.app.Application
import dev.weft.undercurrent.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Application entry. Boots Koin with [appModule]; everything else
 * (runtime, repos, UI singletons, view-models) is constructed lazily on
 * first `get()` / `koinInject()` / `koinViewModel()`.
 *
 * Previously this class held a dozen `lateinit var` fields wired manually
 * in `onCreate`. Koin replaces that:
 *  - Single source of truth for the DI graph lives in [appModule].
 *  - Composables resolve dependencies via `koinInject<T>()` /
 *    `koinViewModel<T>()` instead of casting `LocalContext.current.applicationContext`.
 *  - Tests can swap modules (`startKoin { modules(testModule) }`).
 *
 * Registered via `android:name=".UndercurrentApp"` in the manifest.
 */
class UndercurrentApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            // androidLogger at Info level prints `[Koin] | created instance ...`
            // lines on first resolution — useful while wiring is in flux.
            // Drop to Level.NONE before release once the graph stabilizes.
            androidLogger(Level.INFO)
            androidContext(this@UndercurrentApp)
            modules(appModule)
        }
    }
}

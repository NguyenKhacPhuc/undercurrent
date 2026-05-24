plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

android {
    namespace = "dev.weft.undercurrent"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.weft.undercurrent"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            // Koog transitively pulls in Netty, which ships duplicate META-INF entries.
            // Exclude the ones that don't matter at runtime so the merge step doesn't fail.
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/*.kotlin_module",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
            )
            // Multiple OkHttp 5.x modules (okhttp + okhttp-sse + okhttp-coroutines)
            // each ship the same Java-9-multi-release OSGi manifest. Pick the
            // first; they're identical metadata so the choice is arbitrary.
            pickFirsts += setOf(
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

// SQLDelight — generates the typed query API from src/main/sqldelight/.
// One database for app-owned data (Records). Substrate has its own
// (WeftDatabase) for conversations / memory / traces / usage — they
// stay separate so app and SDK schemas can evolve independently.
sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("dev.weft.undercurrent.db")
        }
    }
}

// Weft SDK — resolved via composite build (`includeBuild(...)` in the root
// `settings.gradle.kts`). The coordinates below match each SDK module's
// declared `group:archivesName`; Gradle substitutes them with the in-tree
// projects from the included SDK build at configuration time. Once the SDK
// publishes to a real Maven repo, swap `includeBuild` for `repositories {}`
// and these lines need no changes.
dependencies {
    // Core agent loop, persistence, streaming, skill primitives, OS-capability
    // tools. Compose-free.
    implementation("dev.weft:weft-android")

    // Compose framework — WeftComponent abstract base, ComposeUiBridge,
    // TreeRenderer. Re-exports compose-bom + ui + foundation.
    implementation("dev.weft:weft-android-compose")

    // Default Material 3 palette + default surfaces + WeftUi convenience
    // holder. Skip this dep if you're rolling your own palette / surfaces.
    implementation("dev.weft:weft-android-compose-defaults")

    // OAuth 2.0 + PKCE client. Used by the Integrations feature to
    // connect MCP servers behind per-user auth (Linear, etc.).
    implementation("dev.weft:weft-oauth")

    // Compose BOM — controls every unversioned Compose dep below. The SDK's
    // `:android-compose` `api()`-exports the same BOM, but BOM platform
    // constraints don't always transit across composite-build edges cleanly.
    // Declare it here too so the app's classpath has a version source for
    // ui-tooling-preview, etc.
    implementation(platform(libs.androidx.compose.bom))

    // material-icons-core ships ~25 common glyphs (Add, Menu, ArrowBack…)
    // — the rest, including Mic for the voice-input button, live in
    // material-icons-extended. R8 tree-shakes unused icons in release
    // builds so the APK size impact is roughly proportional to icon usage.
    implementation(libs.androidx.compose.material.icons.extended)

    // Android entry-point pieces (the SDK is library-only).
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui.tooling.preview)

    // MVI state lives in an AppStore ViewModel. viewmodel-compose gives us
    // `viewModel { ... }`; runtime-compose gives us collectAsStateWithLifecycle.
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Persists theme prefs (palette + Auto/Light/Dark) across launches.
    // Flow-based, async, no synchronous read on the main thread.
    implementation(libs.androidx.datastore.preferences)

    // SQLDelight Android driver — backs the persistent DataSource used
    // by the substrate's data_* tools (notes, tasks, and any tracker
    // data the agent persists). Schema in src/main/sqldelight/.
    implementation(libs.sqldelight.android.driver)
    implementation(libs.sqldelight.coroutines.extensions)

    // Koin DI — replaces hand-wired UndercurrentApp lateinit fields +
    // the old AppStore.factory(...) call site. See di/AppModule.kt.
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Chrome Custom Tabs — used by MarkdownText to open links in an
    // in-app browser tab (returns via back button, faster than spawning
    // a separate app). Falls back to ACTION_VIEW if no CCT-supporting
    // browser is installed.
    implementation(libs.androidx.browser)

    // FragmentActivity for MainActivity — required by androidx.biometric so
    // the Weft BiometricPrompt UI can attach to the host activity.
    implementation(libs.androidx.fragment.ktx)

    // Weft DevTools — debug-only overlay for inspecting the live runtime.
    // Pulled in via `debugImplementation` so the FAB + bottom sheet don't
    // ship in release builds.
    debugImplementation("dev.weft.devtools:weft-android-devtools")
}

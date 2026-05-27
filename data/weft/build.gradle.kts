// :data:weft — Android-only bridge to the Weft SDK.
//
// Weft is Android-only (per the KMP migration decision). To keep
// feature modules KMP-friendly, all Weft access goes through this
// module. Common API: a `WeftEngine` interface in :shared that
// feature modules use; the Android implementation lives here and
// wraps WeftRuntime/WeftAgent. The iOS app ships without agent
// capabilities for v1.
//
// Apply the stock `com.android.library` plugin (not KMP) because
// Weft itself is JVM/Android-only — there's no iosMain to populate.

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.weft.undercurrent.data.weft"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // KMP-shared business contracts (engine interface, model types).
    implementation(projects.shared)
    implementation(projects.core.model)

    // The Weft SDK itself — wired via the `includeBuild("../weft")`
    // composite + dependencySubstitution in settings.gradle.kts.
    api("dev.weft:weft-android")
    api("dev.weft:weft-android-compose")
    api("dev.weft:weft-android-compose-defaults")
    api("dev.weft:weft-oauth")
    debugImplementation("dev.weft.devtools:weft-android-devtools")

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.koin.android)
}

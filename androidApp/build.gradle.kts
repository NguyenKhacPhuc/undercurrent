// :androidApp — the Android entrypoint app. Owns Application,
// MainActivity, AndroidManifest, app-icon, Android-specific theming
// integration. Hosts the shared :composeApp inside setContent { }.
//
// Slim by design — everything that isn't strictly Android-specific
// lives in :composeApp, :shared, or the feature modules. iOS gets
// the equivalent shell in iosApp/ (Xcode project).

plugins {
    alias(libs.plugins.undercurrent.android.application)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.weft.undercurrent"
    defaultConfig {
        applicationId = "dev.weft.undercurrent"
        versionCode = 1
        versionName = "0.0.1"
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(projects.shared)
    implementation(projects.data.weft)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
}

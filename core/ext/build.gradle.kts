// Pure KMP library — Android + iOS, no Compose, no Koin.
// Add module-specific dependencies in the sourceSets block below.

plugins {
    alias(libs.plugins.undercurrent.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // module-specific common deps go here
        }
        androidMain.dependencies {
            // Custom Tabs for openInBrowser — keep the user in-app for
            // BYOK provider-console links and assistant markdown taps.
            implementation(libs.androidx.browser)
        }
        iosMain.dependencies {
            // module-specific iOS deps go here
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.core.ext"
}

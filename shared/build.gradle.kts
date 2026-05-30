// Pure KMP library — Android + iOS, no Compose, no Koin.
// Add module-specific dependencies in the sourceSets block below.

plugins {
    alias(libs.plugins.undercurrent.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            api(libs.androidx.lifecycle.viewmodel)
        }
        androidMain.dependencies {
            // module-specific Android deps go here
        }
        iosMain.dependencies {
            // module-specific iOS deps go here
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.shared"
}

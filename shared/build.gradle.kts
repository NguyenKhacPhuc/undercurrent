// Pure KMP library — Android + iOS, no Compose, no Koin.
// Add module-specific dependencies in the sourceSets block below.

plugins {
    alias(libs.plugins.undercurrent.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Gateway interfaces in commonMain reference ProviderKind +
            // ModelTier — keep :core:model as an API dependency so
            // feature modules pick them up transitively from :shared.
            api(projects.core.model)
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

plugins {
    alias(libs.plugins.undercurrent.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)
            implementation(projects.core.domain)
            implementation(projects.data.network)
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.feature.auth"
}

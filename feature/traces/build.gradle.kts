// Feature module — KMP + Compose Multiplatform + Koin.
// Automatically depends on :core:model, :core:ui, :core:design-system,
// :core:navigation, :core:resources via the feature convention plugin.
//
// Add feature-specific dependencies (data modules, ML Kit, etc.) below.

plugins {
    alias(libs.plugins.undercurrent.kmp.feature)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Gateway interfaces (TraceStoreGateway + AgentTrace/* mirrors).
            implementation(projects.core.domain)
            implementation(projects.shared)
            // HH:mm:ss.SSS formatting in trace rows + meta blocks.
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            // Substrate — WeftTraceExportViewModel writes traces to a
            // file via the runtime's OS bridges and shares via the
            // system sheet.
            implementation("dev.weft:weft-runtime")
            implementation("dev.weft:weft-harness-observability")
            implementation("dev.weft:weft-contracts")
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.feature.traces"
}

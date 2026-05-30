pluginManagement {
    // Convention plugins live in `build-logic/` and are made available
    // to every module via the includeBuild below. Module build.gradle.kts
    // files reference them as `alias(libs.plugins.undercurrent.kmp.feature)`.
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "undercurrent"

includeBuild("../weft") {
    dependencySubstitution {
        substitute(module("dev.weft:weft-runtime"))
            .using(project(":runtime"))
        substitute(module("dev.weft:weft-compose"))
            .using(project(":compose"))
        substitute(module("dev.weft:weft-compose-defaults"))
            .using(project(":compose-defaults"))
        substitute(module("dev.weft:weft-oauth"))
            .using(project(":oauth"))
        substitute(module("dev.weft.devtools:weft-devtools"))
            .using(project(":devtools"))

        // KMP `:contracts` — pure-Kotlin interfaces (DataSource,
        // FileSaveSpec, OsCapabilities, …) consumed by undercurrent's
        // KMP-published data modules.
        substitute(module("dev.weft:weft-contracts"))
            .using(project(":contracts"))

        // KMP-published harness modules. Required by composeApp's
        // iosMain so it can build a substrate-backed WeftAgent.
        substitute(module("dev.weft:weft-harness-agents"))
            .using(project(":harness:agents"))
        substitute(module("dev.weft:weft-harness-prompt"))
            .using(project(":harness:prompt"))
        substitute(module("dev.weft:weft-harness-observability"))
            .using(project(":harness:observability"))
        substitute(module("dev.weft:weft-harness-conversation"))
            .using(project(":harness:conversation"))
        substitute(module("dev.weft:weft-harness-cost"))
            .using(project(":harness:cost"))
        substitute(module("dev.weft:weft-harness-memory"))
            .using(project(":harness:memory"))
        substitute(module("dev.weft:weft-harness-reliability"))
            .using(project(":harness:reliability"))
        substitute(module("dev.weft:weft-harness-behavior"))
            .using(project(":harness:behavior"))
        substitute(module("dev.weft:weft-tools"))
            .using(project(":tools"))
    }
}

// =============================================================
// Entry-point modules
// =============================================================
include(":androidApp")        // Android app shell (Application + MainActivity)
include(":composeApp")        // CMP shared UI surface
// include(":iosApp")         // iOS Xcode project — not a Gradle module
include(":shared")            // KMP shared business logic

// =============================================================
// Core modules — shared infrastructure
// =============================================================
include(":core:model")           // domain types (KMP, pure Kotlin)
include(":core:ui")              // shared composables (loading, error, …)
include(":core:design-system")   // colors, typography, spacing tokens
include(":core:navigation")      // navigation primitives
include(":core:resources")       // strings, drawables, icons
include(":core:domain")          // repositories + use cases
include(":core:ext")             // Kotlin extensions

// =============================================================
// Data modules — persistence + network + integrations
// =============================================================
include(":data:datastore")       // DataStore-Preferences factory (commonMain + per-platform path builders)
include(":data:sqldelight")      // SQLDelight schema + drivers
include(":data:network")         // Ktor HTTP client (when needed outside Weft)
include(":data:weft")            // Weft bridge — Android-only; wraps WeftRuntime

// =============================================================
// Feature modules — one per screen / flow
// =============================================================
include(":feature:chat")
include(":feature:conversations")
include(":feature:memories")
include(":feature:personas")
include(":feature:traces")
include(":feature:settings")
include(":feature:onboarding")
include(":feature:providers")
include(":feature:voice")
include(":feature:maps")
include(":feature:creator")
include(":feature:theme")
include(":feature:usage")
include(":feature:miniapps")
include(":feature:keypaste")
include(":feature:integrations")
include(":feature:navigation")

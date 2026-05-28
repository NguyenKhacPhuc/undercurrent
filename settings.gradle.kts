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

// Type-safe project accessors — modules reference each other as
// `implementation(projects.core.model)` instead of `:core:model`.
// Match r10's convention.
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "undercurrent"

// =============================================================
// Weft SDK — composite build from sibling directory.
// =============================================================
// Source: https://github.com/NguyenKhacPhuc/android-harness
// Expected layout:
//   <parent>/weft/           (Weft SDK)
//   <parent>/undercurrent/   (this app)
//
// Weft is Android-only (per the KMP migration decision: Weft stays
// Android-only; Undercurrent's `:data:weft` module is the bridge).
includeBuild("../weft") {
    dependencySubstitution {
        substitute(module("dev.weft:weft-android"))
            .using(project(":android"))
        substitute(module("dev.weft:weft-android-compose"))
            .using(project(":android-compose"))
        substitute(module("dev.weft:weft-android-compose-defaults"))
            .using(project(":android-compose-defaults"))
        substitute(module("dev.weft:weft-oauth"))
            .using(project(":oauth"))
        substitute(module("dev.weft.devtools:weft-android-devtools"))
            .using(project(":android-devtools"))
    }
}

// =============================================================
// Entry-point modules
// =============================================================
include(":androidApp")        // Android app shell (Application + MainActivity)
include(":composeApp")        // CMP shared UI surface
// include(":iosApp")         // iOS Xcode project — not a Gradle module
include(":shared")            // KMP shared business logic
include(":sharedCatalog")     // KMP shared design tokens (for sibling apps)

// =============================================================
// Core modules — shared infrastructure
// =============================================================
include(":core:model")           // domain types (KMP, pure Kotlin)
include(":core:ui")              // shared composables (loading, error, …)
include(":core:design-system")   // colors, typography, spacing tokens
include(":core:navigation")      // navigation primitives
include(":core:resources")       // strings, drawables, icons
include(":core:domain")          // use cases
include(":core:ext")             // Kotlin extensions

// =============================================================
// Data modules — persistence + network + integrations
// =============================================================
include(":data:repository")      // top-level repositories (KMP common API)
include(":data:datastore")       // preferences (multiplatform-settings or platform-specific)
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

// UI modules — Android-only Compose surfaces.
include(":ui:components")        // WeftComponent palette for ui_render (Android-only)

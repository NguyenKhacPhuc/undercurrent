// build-logic is an included build: its convention plugins are
// available to the root project's modules via the `plugins { id(...) }`
// block. Loaded from the root `settings.gradle.kts` via
// `pluginManagement { includeBuild("build-logic") }`.
//
// The build catalog is shared from the root via `versionCatalogs`
// below, so convention plugins can look up library versions by name
// (same `libs` accessor module authors use).

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")

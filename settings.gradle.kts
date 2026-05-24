pluginManagement {
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

rootProject.name = "undercurrent"

// Composite build wiring — the Weft SDK lives in a sibling directory.
//
// Why the `dependencySubstitution` block: composite-build auto-substitution
// matches by `group:project.name`, where `project.name` is the *directory*
// name (`:android` → "android"). Each Weft module's `archivesName.set(...)`
// gives it a friendlier published artifact name (`weft-android`), but
// Gradle's internal composite-build resolver doesn't honor that. So we
// declare explicit substitutions that map the friendly coordinates to the
// real project paths.
//
// Layout expected:
//   ~/Documents/mas/mobile-agent-substrate/  (Weft SDK repo)
//   ~/Documents/mas/undercurrent/            (this app repo)
includeBuild("../weft") {
    dependencySubstitution {
        substitute(module("dev.weft:weft-android"))
            .using(project(":android"))
        substitute(module("dev.weft:weft-android-compose"))
            .using(project(":android-compose"))
        substitute(module("dev.weft:weft-android-compose-defaults"))
            .using(project(":android-compose-defaults"))
        // Opt-in debug overlay. Apps usually pull this in via
        // `debugImplementation` so the FAB doesn't ship in release builds.
        substitute(module("dev.weft.devtools:weft-android-devtools"))
            .using(project(":android-devtools"))
        // Add more lines here if the app starts depending on SDK modules
        // directly (e.g. `dev.weft:weft-contracts` → `project(":contracts")`).
        // Apps typically only touch the top-level weft-android* artifacts;
        // everything else transits as `api()` deps.
    }
}

include(":app")

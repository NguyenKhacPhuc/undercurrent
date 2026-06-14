import jetbrains.buildServer.configs.kotlin.*

/*
 * Undercurrent — TeamCity versioned settings (Kotlin DSL, portable format).
 *
 * Keep `version` matching the <version> in pom.xml and your TeamCity server.
 * The server rewrites both on import.
 *
 * Pipeline shape:
 *   BuildAndroid ───┐
 *                   ├──► PrValidation (composite gate, triggered on PRs)
 *   BuildIos ───────┘
 *   AssembleDebugApk   (debug APK artifact)
 *   UatRelease         (UAT distribution build, main branch)
 *   PublishPlayConsole / PublishAppStore   (next-phase scaffolds, paused)
 *
 * Composite-build caveat: undercurrent's settings.gradle.kts does
 * includeBuild("../weft"). CI therefore checks out BOTH repos as siblings:
 *   <checkout>/undercurrent   (this repo)   +   <checkout>/weft   (android-harness)
 * and runs Gradle with workingDir = "undercurrent". See sharedComposeCheckout().
 */

version = "2024.12"

project {
    description = "Undercurrent CI — KMP personal-assistant app (Android + iOS)"

    // VCS roots already exist on the server — referenced by ID from Common.kt,
    // not redefined here. See UNDERCURRENT_VCS_ID / WEFT_VCS_ID.

    buildType(BuildAndroid)
    buildType(BuildIos)
    buildType(AssembleDebugApk)
    buildType(PrValidation)
    buildType(UatRelease)
    buildType(PublishPlayConsole)
    buildType(PublishAppStore)

    params {
        // JDK 17 on the agent. TeamCity bundled agents expose JDK paths as
        // env.JDK_17_0 / env.JDK_17 — adjust to whatever your agents publish,
        // or set a custom agent property. Used as `jdkHome` in every Gradle step.
        param("jdk.home", "%env.JDK_17_0%")

        // Token for the PR commit-status publisher only (VCS auth comes from the
        // existing server-managed roots). Define `github.token` as a PASSWORD
        // param in the project UI — do NOT commit a real token. Remove this if
        // such a param already exists at a higher scope.
        password("github.token", "credentialsJSON:REPLACE_WITH_TOKEN", display = ParameterDisplay.HIDDEN)
    }
}

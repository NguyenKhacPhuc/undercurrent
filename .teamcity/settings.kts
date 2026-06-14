import jetbrains.buildServer.configs.kotlin.*

/*
 * Undercurrent — TeamCity versioned settings (Kotlin DSL, portable format).
 *
 * Keep `version` matching the <version> in pom.xml and your TeamCity server.
 * The server rewrites both on import.
 *
 * Pipeline:
 *   PrValidation       — preflight -> lint -> test -> build(debug apk + ios). On PRs.
 *   Publish            — release-candidate: lint + test + build. Manual.
 *   DeployFirebase     — signed release APK -> Firebase App Distribution. On main.
 *   DeployPlayConsole  — signed release AAB -> Play Console. On release* branches.
 *   DeployAppStore     — iosApp archive -> App Store Connect. On release*, macOS.
 * Deploy configs build + upload via fastlane — see docs/deployment.md.
 *
 * weft is consumed as a published artifact (dev.weft:weft-* from GitHub
 * Packages), NOT a composite build, so CI uses a single root and builds at
 * the repo root. The agent needs read:packages creds — see env.GITHUB_* below.
 */

version = "2025.03"

project {
    description = "Undercurrent CI — KMP personal-assistant app (Android + iOS)"

    // The VCS root already exists on the server — referenced by ID from
    // Common.kt (UNDERCURRENT_VCS_ID), not redefined here.

    buildType(PrValidation)
    buildType(Publish)
    buildType(DeployFirebase)
    buildType(DeployPlayConsole)
    buildType(DeployAppStore)

    params {
        // JDK 17 on the agent. TeamCity bundled agents expose JDK paths as
        // env.JDK_17_0 / env.JDK_17 — adjust to whatever your agents publish,
        // or set a custom agent property. Used as `jdkHome` in every Gradle step.
        param("jdk.home", "%env.JDK_17_0%")

        // GitHub Packages read creds — Gradle pulls dev.weft:weft-* from them.
        // Injected as env vars so settings.gradle.kts' System.getenv() fallback
        // picks them up. `github.username` is plain; `github.token` is a SECRET
        // added in the UI (Project → Parameters → Password) with read:packages
        // (+ repo:status for the PR publisher). Don't declare the secret here —
        // TeamCity writes the credentialsJSON token back on sync.
        param("github.username", "NguyenKhacPhuc")
        param("env.GITHUB_ACTOR", "%github.username%")
        param("env.GITHUB_TOKEN", "%github.token%")
    }
}

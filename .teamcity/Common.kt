import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script

/*
 * The undercurrent VCS root already exists on the server — referenced by id.
 * weft is no longer checked out here: it's consumed as a published artifact
 * (dev.weft:weft-*) from GitHub Packages, so CI needs a single root only.
 */
const val UNDERCURRENT_VCS_ID = "Undercurent_Undercurrent"

/** Single-root checkout at the repo root (weft comes from GitHub Packages). */
fun BuildType.undercurrentCheckout() {
    vcs {
        root(AbsoluteId(UNDERCURRENT_VCS_ID))
        cleanCheckout = true
    }
}

/** Preflight: report the toolchain so build logs show what ran. */
fun BuildSteps.preflightStep() {
    script {
        name = "preflight · toolchain"
        scriptContent = "java -version; xcodebuild -version || true; ./gradlew --version | grep -i gradle || true"
    }
}

/** Gradle step at the repo root, using the wrapper + agent JDK 17. */
fun BuildSteps.gradleStep(stepName: String, gradleTasks: String) {
    gradle {
        name = stepName
        tasks = gradleTasks
        useGradleWrapper = true
        gradleWrapperPath = ""
        jdkHome = "%jdk.home%"
        // --no-configuration-cache: TeamCity's gradle-runner init script
        // registers build listeners, which the config cache (on in
        // gradle.properties) rejects under Gradle 9. The daemon is kept (a
        // persistent self-hosted agent reuses it across the staged steps).
        gradleParams = "--stacktrace --no-configuration-cache"
    }
}

/** Runs a fastlane lane (installs the Gemfile bundle first). */
fun BuildSteps.fastlaneStep(stepName: String, lane: String) {
    script {
        name = stepName
        scriptContent = "bundle install --quiet && bundle exec fastlane $lane"
    }
}

/** Agents that can build Android need the SDK on PATH/env. */
fun BuildType.requireAndroidSdk() {
    requirements {
        exists("env.ANDROID_HOME")
    }
}

/** iOS Kotlin/Native compilation needs a macOS agent. */
fun BuildType.requireMacOs() {
    requirements {
        contains("teamcity.agent.jvm.os.name", "Mac")
    }
}

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

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

/** Gradle step at the repo root, using the wrapper + agent JDK 17. */
fun BuildSteps.gradleStep(stepName: String, gradleTasks: String) {
    gradle {
        name = stepName
        tasks = gradleTasks
        useGradleWrapper = true
        gradleWrapperPath = ""
        jdkHome = "%jdk.home%"
        gradleParams = "--no-daemon --stacktrace"
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

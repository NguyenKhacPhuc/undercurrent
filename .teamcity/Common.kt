import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle

/*
 * VCS roots are NOT defined in this DSL — they already exist on the server.
 * Put their "VCS root ID" (TeamCity → VCS Root → identifier) here and every
 * build type references them via AbsoluteId. The weft root must point at the
 * android-harness repo, since undercurrent composite-includes it.
 */
const val UNDERCURRENT_VCS_ID = "Undercurent_Undercurrent"
const val WEFT_VCS_ID = "Weft_Weft"

/**
 * Lays out both repos as siblings so includeBuild("../weft") resolves:
 *
 *   <checkout>/undercurrent   <- this repo   (Gradle workingDir)
 *   <checkout>/weft           <- android-harness
 */
fun BuildType.sharedComposeCheckout() {
    vcs {
        root(AbsoluteId(UNDERCURRENT_VCS_ID), "+:. => undercurrent")
        root(AbsoluteId(WEFT_VCS_ID), "+:. => weft")
        cleanCheckout = true
    }
}

/** Gradle step rooted in the undercurrent subdir, using the wrapper + agent JDK 17. */
fun BuildSteps.gradleStep(stepName: String, gradleTasks: String) {
    gradle {
        name = stepName
        tasks = gradleTasks
        workingDir = "undercurrent"
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

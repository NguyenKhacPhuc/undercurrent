import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.vcs

/**
 * UAT distribution build off main. Today it assembles the debug APK as the
 * stand-in artifact; once a signed `uatRelease` variant + signing config land
 * (next phase), repoint `uat.gradle.tasks` and the artifact rule.
 */
object UatRelease : BuildType({
    name = "UAT · assemble"
    description = "Builds the UAT artifact for distribution off main."

    params {
        // TODO(next phase): switch to ":androidApp:assembleUatRelease" (or bundleUatRelease)
        // once the UAT build variant + signing config exist.
        param("uat.gradle.tasks", ":androidApp:assembleDebug")
    }

    artifactRules = "androidApp/build/outputs/apk/**/*.apk => uat"

    undercurrentCheckout()

    steps {
        gradleStep("assemble UAT", "%uat.gradle.tasks%")
    }

    triggers {
        vcs {
            branchFilter = "+:refs/heads/main"
        }
    }

    requireAndroidSdk()
})

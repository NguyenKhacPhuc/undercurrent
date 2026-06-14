import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.vcs

/**
 * Android beta distribution. Builds the DEBUG APK (debug signing is fine for
 * tester distribution — no release keystore needed) and uploads it to Firebase
 * App Distribution via fastlane. Auto-triggers on main.
 *
 * Secrets/ids below reference params you define in the UI. See docs/deployment.md.
 */
object DeployFirebase : BuildType({
    name = "Deploy · Firebase App Distribution (android beta)"
    description = "Builds the debug APK and distributes it to Firebase App Distribution. On main."

    params {
        param("env.JAVA_HOME", "%jdk.home%")
        param("env.BUILD_NUMBER", "%build.number%")
        param("env.VERSION_CODE", "%build.counter%")   // monotonic -> Android versionCode
        // Firebase App Distribution
        param("env.FIREBASE_APP_ID", "%firebase.app.id%")
        param("env.FIREBASE_SERVICE_CREDENTIALS", "%firebase.service.credentials.path%")
        param("env.FIREBASE_GROUPS", "testers")
    }

    undercurrentCheckout()

    steps {
        preflightStep()
        // Firebase App Distribution Gradle plugin — builds + uploads the debug APK.
        gradleStep("build + distribute · firebase", ":androidApp:assembleDebug :androidApp:appDistributionUploadDebug")
    }

    triggers {
        vcs {
            branchFilter = "+:<default>"   // main
        }
    }

    requireAndroidSdk()
})

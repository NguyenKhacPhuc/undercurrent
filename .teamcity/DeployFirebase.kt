import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.vcs

/**
 * Android beta distribution. Builds the UAT APK (debug-signed, .uat
 * applicationId so it installs alongside production) and uploads it to Firebase
 * App Distribution via the Gradle plugin. Auto-triggers on main.
 *
 * Secrets/ids below reference params you define in the UI. See docs/deployment.md.
 */
object DeployFirebase : BuildType({
    name = "Deploy · Firebase App Distribution (android uat)"
    description = "Builds the UAT APK and distributes it to Firebase App Distribution. On main."

    params {
        param("env.JAVA_HOME", "%jdk.home%")
        param("env.BUILD_NUMBER", "%build.number%")
        param("env.VERSION_CODE", "%build.counter%")   // monotonic -> Android versionCode
        // Firebase App Distribution
        param("env.FIREBASE_APP_ID", "%firebase.app.id%")
        // Paste the service-account JSON content into the secret param (no file
        // on the agent); the build materializes it. (A FIREBASE_SERVICE_CREDENTIALS
        // path still wins if you'd rather use a file.)
        param("env.FIREBASE_SERVICE_CREDENTIALS_JSON", "%firebase.service.credentials.json%")
        param("env.FIREBASE_GROUPS", "testers")
        // GOOGLE_SERVICES_JSON_B64 is set at the project level (inherited here).
    }

    undercurrentCheckout()

    steps {
        preflightStep()
        // Firebase App Distribution Gradle plugin — builds + uploads the UAT APK.
        gradleStep("build + distribute · firebase", ":androidApp:assembleUat :androidApp:appDistributionUploadUat")
    }

    triggers {
        vcs {
            branchFilter = "+:<default>"   // main
        }
    }

    requireAndroidSdk()
})

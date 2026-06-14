import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.vcs

/**
 * Android beta distribution. Builds a signed release APK and uploads it to
 * Firebase App Distribution via fastlane. Auto-triggers on main.
 *
 * Secrets/ids below reference params you define in the UI (Password params for
 * secrets, secure-file paths for the keystore + service-account JSON). See
 * docs/deployment.md.
 */
object DeployFirebase : BuildType({
    name = "Deploy · Firebase App Distribution (android beta)"
    description = "Builds a signed release APK and distributes it to Firebase App Distribution. Manual."

    params {
        param("env.JAVA_HOME", "%jdk.home%")
        param("env.BUILD_NUMBER", "%build.number%")
        param("env.VERSION_CODE", "%build.counter%")   // monotonic -> Android versionCode
        // Android release signing
        param("env.RELEASE_KEYSTORE", "%release.keystore.path%")
        param("env.RELEASE_KEYSTORE_PASSWORD", "%release.keystore.password%")
        param("env.RELEASE_KEY_ALIAS", "%release.key.alias%")
        param("env.RELEASE_KEY_PASSWORD", "%release.key.password%")
        // Firebase App Distribution
        param("env.FIREBASE_APP_ID", "%firebase.app.id%")
        param("env.FIREBASE_SERVICE_CREDENTIALS", "%firebase.service.credentials.path%")
        param("env.FIREBASE_GROUPS", "testers")
    }

    undercurrentCheckout()

    steps {
        preflightStep()
        fastlaneStep("fastlane · android firebase", "android firebase")
    }

    triggers {
        vcs {
            branchFilter = "+:<default>"   // main
        }
    }

    requireAndroidSdk()
})

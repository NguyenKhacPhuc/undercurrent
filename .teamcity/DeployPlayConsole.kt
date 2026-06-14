import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.vcs

/**
 * Android store release. Builds a signed release AAB and uploads it to the
 * Google Play Console (draft on the chosen track) via fastlane supply.
 * Auto-triggers on release* branches.
 *
 * Secrets/ids reference UI-defined params — see docs/deployment.md.
 */
object DeployPlayConsole : BuildType({
    name = "Deploy · Google Play Console (android store)"
    description = "Builds a signed release AAB and uploads it to Play Console as a draft. Manual."

    params {
        param("env.JAVA_HOME", "%jdk.home%")
        param("env.VERSION_CODE", "%build.counter%")   // monotonic -> Android versionCode
        // Android release signing
        param("env.RELEASE_KEYSTORE", "%release.keystore.path%")
        param("env.RELEASE_KEYSTORE_PASSWORD", "%release.keystore.password%")
        param("env.RELEASE_KEY_ALIAS", "%release.key.alias%")
        param("env.RELEASE_KEY_PASSWORD", "%release.key.password%")
        // Google Play
        param("env.PLAY_SERVICE_ACCOUNT_JSON", "%play.service.account.json.path%")
        // internal | alpha | beta | production
        param("env.PLAY_TRACK", "internal")
    }

    undercurrentCheckout()

    steps {
        preflightStep()
        fastlaneStep("fastlane · android play", "android play")
    }

    triggers {
        vcs {
            branchFilter = "+:release*"
        }
    }

    requireAndroidSdk()
})

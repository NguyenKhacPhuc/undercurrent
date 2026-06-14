import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.vcs

/**
 * iOS store release. Archives iosApp and uploads it to App Store Connect
 * (TestFlight + store) via fastlane, authenticated with an App Store Connect
 * API key. macOS agent with Xcode required. Auto-triggers on release* branches.
 *
 * Secrets/ids reference UI-defined params — see docs/deployment.md.
 */
object DeployAppStore : BuildType({
    name = "Deploy · App Store Connect (ios store)"
    description = "Archives iosApp and uploads to App Store Connect via fastlane. macOS + Xcode. Manual."

    params {
        param("env.BUILD_NUMBER", "%build.counter%")   // monotonic -> iOS CFBundleVersion
        // App Store Connect API key (.p8 + ids)
        param("env.ASC_KEY_ID", "%asc.key.id%")
        param("env.ASC_ISSUER_ID", "%asc.issuer.id%")
        param("env.ASC_KEY_PATH", "%asc.key.path%")
    }

    undercurrentCheckout()

    steps {
        preflightStep()
        fastlaneStep("fastlane · ios appstore", "ios appstore")
    }

    triggers {
        vcs {
            branchFilter = "+:release*"
        }
    }

    requireMacOs()
})

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.script

/**
 * NEXT-PHASE SCAFFOLD — Apple App Store / TestFlight publishing.
 *
 * macOS-only, manual-trigger, guarded by `publish.enabled`. To turn it on:
 *   1. Stand up the iosApp archive/export flow (xcodebuild or fastlane).
 *   2. Provision signing certs + App Store Connect API key on the mac agent.
 *   3. Set publish.enabled=true and replace the placeholder step with the
 *      real `fastlane` / `xcodebuild -exportArchive` invocation.
 */
object PublishAppStore : BuildType({
    name = "Publish · App Store / TestFlight (next phase)"
    description = "Scaffold. Archives + uploads the iOS build. Disabled until signing + App Store Connect API key are configured."

    params {
        param("publish.enabled", "false")
    }

    sharedComposeCheckout()

    steps {
        script {
            name = "guard — next phase"
            scriptContent = """
                if [ "%publish.enabled%" != "true" ]; then
                  echo "App Store publishing is a next-phase scaffold (publish.enabled=false)."
                  echo "See .teamcity/PublishAppStore.kt for the enablement checklist."
                  exit 1
                fi
                echo "TODO: cd undercurrent/iosApp && fastlane release"
            """.trimIndent()
        }
    }

    requireMacOs()
})

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.script

/**
 * NEXT-PHASE SCAFFOLD — Google Play publishing.
 *
 * Manual-trigger only and guarded by `publish.enabled`. To turn it on:
 *   1. Add the Gradle Play Publisher plugin to :androidApp.
 *   2. Upload the Play service-account JSON as a secure file / param.
 *   3. Wire the release signing config.
 *   4. Set publish.enabled=true and repoint the Gradle tasks below.
 */
object PublishPlayConsole : BuildType({
    name = "Publish · Google Play (next phase)"
    description = "Scaffold. Uploads the release AAB to Play Console. Disabled until signing + service account are configured."

    params {
        param("publish.enabled", "false")
        // e.g. ":androidApp:publishReleaseBundle" once Play Publisher is wired.
        param("play.gradle.tasks", ":androidApp:bundleRelease")
    }

    sharedComposeCheckout()

    steps {
        script {
            name = "guard — next phase"
            scriptContent = """
                if [ "%publish.enabled%" != "true" ]; then
                  echo "Play Console publishing is a next-phase scaffold (publish.enabled=false)."
                  echo "See .teamcity/PublishPlayConsole.kt for the enablement checklist."
                  exit 1
                fi
            """.trimIndent()
        }
        gradleStep("bundle + publish (Play)", "%play.gradle.tasks%")
    }

    requireAndroidSdk()
})

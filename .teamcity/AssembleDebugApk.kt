import jetbrains.buildServer.configs.kotlin.*

object AssembleDebugApk : BuildType({
    name = "Android · assemble debug APK"
    description = "Builds the debug APK and publishes it as a downloadable artifact."

    artifactRules = "undercurrent/androidApp/build/outputs/apk/debug/*.apk => apk"

    sharedComposeCheckout()

    steps {
        gradleStep("assembleDebug", ":androidApp:assembleDebug")
    }

    requireAndroidSdk()
})

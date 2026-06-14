import jetbrains.buildServer.configs.kotlin.*

object AssembleDebugApk : BuildType({
    name = "Android · assemble debug APK"
    description = "Builds the debug APK and publishes it as a downloadable artifact."

    artifactRules = "androidApp/build/outputs/apk/debug/*.apk => apk"

    undercurrentCheckout()

    steps {
        gradleStep("assembleDebug", ":androidApp:assembleDebug")
    }

    requireAndroidSdk()
})

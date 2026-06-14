import jetbrains.buildServer.configs.kotlin.*

/**
 * Release pipeline — full validation then deployment. Manual trigger.
 *   preflight -> quality -> test -> build -> deploy
 * The deploy stage (beta + store) is gated behind `publish.enabled` until
 * signing keys, Fastlane, and the Firebase / Play / App Store Connect
 * credentials are provisioned. Until then each deploy step logs its TODO and
 * skips (publish.enabled=false) — turning it on without the wiring fails fast.
 */
object Publish : BuildType({
    name = "Publish · build + test + lint + deploy"
    description = "Validates, then deploys to beta (Firebase / TestFlight) and stores (Play / App Store). Manual."

    params {
        param("publish.enabled", "false")
    }

    artifactRules = """
        androidApp/build/outputs/**/*.apk => apk
        androidApp/build/outputs/**/*.aab => bundle
        **/build/reports/tests/** => test-reports
    """.trimIndent()

    undercurrentCheckout()

    steps {
        // Stage 1 — environment
        preflightStep()
        // Stage 2 — code quality
        gradleStep("quality · lint", "lint")
        // Stage 2 — testing (android unit/kotest + ios simulator + coverage)
        gradleStep("test · android + ios", "testDebugUnitTest iosSimulatorArm64Test koverXmlReport")
        // Stage 3 — build: release Android bundle + iOS framework. Needs the
        // signing config; until that exists this proves a debug build compiles.
        gradleStep("build · android + ios", ":androidApp:assembleDebug :composeApp:compileKotlinIosSimulatorArm64")

        // Stage 4 — deploy (gated scaffolds; see Common.kt::gatedDeployStep)
        gatedDeployStep(
            "deploy · Firebase App Distribution (android beta)",
            "build signed release APK/AAB, upload via Gradle Play Publisher's firebaseAppDistribution or the Firebase CLI",
        )
        gatedDeployStep(
            "deploy · TestFlight (ios beta)",
            "archive iosApp via xcodebuild/fastlane, upload to TestFlight with an App Store Connect API key",
        )
        gatedDeployStep(
            "deploy · Google Play Console (android store)",
            "bundleRelease + sign, then publishReleaseBundle (Gradle Play Publisher) with a Play service account",
        )
        gatedDeployStep(
            "deploy · App Store Connect (ios store)",
            "fastlane deliver / xcrun altool upload of the signed .ipa to App Store Connect",
        )
    }

    requireMacOs()
    requireAndroidSdk()
})

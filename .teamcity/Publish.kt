import jetbrains.buildServer.configs.kotlin.*

/**
 * Release-candidate build: full validation + release artifacts. Manual.
 *   preflight -> quality -> test -> build
 * Actual deployment lives in dedicated configs (DeployFirebase /
 * DeployPlayConsole / DeployAppStore), each of which builds + uploads via
 * fastlane. This config is the "is the release green?" gate.
 */
object Publish : BuildType({
    name = "Publish · build + test + lint"
    description = "Release-candidate validation: lint, tests, and a release build. Deployment is in the Deploy · * configs."

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
        // Stage 2 — testing (android unit/kotest + coverage)
        gradleStep("test · android", "testDebugUnitTest koverXmlReport")
        // Stage 3 — build (debug apk + iOS compile; release signing lives in the
        // Deploy configs, which build the signed release artifacts they upload)
        gradleStep("build · android + ios", ":androidApp:assembleDebug :composeApp:compileKotlinIosSimulatorArm64")
    }

    requireMacOs()
    requireAndroidSdk()
})

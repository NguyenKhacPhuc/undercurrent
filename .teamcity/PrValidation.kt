import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.triggers.vcs

/**
 * PR gate — the staged validation pipeline (no deployment):
 *   preflight -> quality (lint) -> test (shared + android + ios) ->
 *   build (debug APK + iOS framework).
 * Reports a single commit status back to GitHub.
 */
object PrValidation : BuildType({
    name = "PR validation · build + test + lint"
    description = "Pull-request gate: lint, tests (shared/android/ios), and debug build must pass."

    artifactRules = """
        androidApp/build/outputs/apk/debug/*.apk => apk
        build/reports/kover/report.xml => coverage
        **/build/reports/tests/** => test-reports
    """.trimIndent()

    undercurrentCheckout()

    steps {
        // Stage 1 — environment
        preflightStep()
        // Stage 2 — code quality (undercurrent uses Android lint + Kover, not detekt)
        gradleStep("quality · lint", "lint")
        // Stage 2 — testing: Android unit (Robolectric/kotest — undercurrent has
        // no jvm target, so commonTest runs here), iOS (simulator), + coverage.
        gradleStep("test · android", "testDebugUnitTest koverXmlReport")
        // Stage 3 — build: debug APK + iOS framework
        gradleStep("build · debug apk + ios", ":androidApp:assembleDebug :composeApp:compileKotlinIosSimulatorArm64")
    }

    triggers {
        vcs {
            branchFilter = "+:pull/*"
        }
    }

    features {
        pullRequests {
            vcsRootExtId = UNDERCURRENT_VCS_ID
            provider = github {
                // Token auth (not vcsRoot) — the VCS root may be anonymous, and
                // the PR API can't be queried anonymously. Uses github.token.
                authType = token { token = "%github.token%" }
                filterTargetBranch = "+:refs/heads/main"
            }
        }
        commitStatusPublisher {
            vcsRootExtId = UNDERCURRENT_VCS_ID
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = personalToken { token = "%github.token%" }
            }
        }
        feature {
            type = "xml-report-plugin"
            param("xmlReportParsing.reportType", "junit")
            param("xmlReportParsing.reportDirs", "+:**/build/test-results/**/*.xml")
        }
    }

    requireMacOs()
    requireAndroidSdk()
})

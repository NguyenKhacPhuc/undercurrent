import jetbrains.buildServer.configs.kotlin.*

object BuildAndroid : BuildType({
    name = "Android · compile + unit tests + coverage"
    description = "Compiles Android/JVM, runs all unit tests, generates Kover XML coverage."

    artifactRules = """
        undercurrent/build/reports/kover/report.xml => coverage
        undercurrent/**/build/reports/tests/** => test-reports
    """.trimIndent()

    sharedComposeCheckout()

    steps {
        gradleStep(
            "compile + test + kover",
            ":androidApp:compileDebugKotlin test koverXmlReport",
        )
    }

    features {
        feature {
            type = "xml-report-plugin"
            param("xmlReportParsing.reportType", "junit")
            param("xmlReportParsing.reportDirs", "+:undercurrent/**/build/test-results/**/*.xml")
        }
    }

    requireAndroidSdk()
})

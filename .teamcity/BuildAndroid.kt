import jetbrains.buildServer.configs.kotlin.*

object BuildAndroid : BuildType({
    name = "Android · compile + unit tests + coverage"
    description = "Compiles Android/JVM, runs all unit tests, generates Kover XML coverage."

    artifactRules = """
        build/reports/kover/report.xml => coverage
        **/build/reports/tests/** => test-reports
    """.trimIndent()

    undercurrentCheckout()

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
            param("xmlReportParsing.reportDirs", "+:**/build/test-results/**/*.xml")
        }
    }

    requireAndroidSdk()
})

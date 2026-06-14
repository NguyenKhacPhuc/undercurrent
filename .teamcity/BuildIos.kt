import jetbrains.buildServer.configs.kotlin.*

object BuildIos : BuildType({
    name = "iOS · compile (SimulatorArm64)"
    description = "Compiles the shared Kotlin/Native iOS target. Requires a macOS agent."

    undercurrentCheckout()

    steps {
        gradleStep(
            "compile iOS",
            ":composeApp:compileKotlinIosSimulatorArm64",
        )
    }

    requireMacOs()
})

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.triggers.vcs

/**
 * PR gate. A composite build that goes green only when both the Android
 * (compile + tests + coverage) and iOS (compile) builds pass. Reports a
 * single commit status back to GitHub.
 */
object PrValidation : BuildType({
    name = "PR validation"
    description = "Gate for pull requests: Android compile+tests+coverage and iOS compile must pass."
    type = BuildTypeSettings.Type.COMPOSITE

    vcs {
        root(AbsoluteId(UNDERCURRENT_VCS_ID))
        showDependenciesChanges = true
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
                // Reuses the existing VCS root's stored credentials.
                authType = vcsRoot()
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
    }

    dependencies {
        snapshot(BuildAndroid) {
            onDependencyFailure = FailureAction.FAIL_TO_START
            onDependencyCancel = FailureAction.CANCEL
        }
        snapshot(BuildIos) {
            onDependencyFailure = FailureAction.FAIL_TO_START
            onDependencyCancel = FailureAction.CANCEL
        }
    }
})

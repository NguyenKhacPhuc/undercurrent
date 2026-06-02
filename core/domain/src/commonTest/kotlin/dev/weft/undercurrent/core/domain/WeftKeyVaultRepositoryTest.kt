package dev.weft.undercurrent.core.domain

import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import dev.weft.android.WeftRuntime
import dev.weft.contracts.KeyVault
import dev.weft.undercurrent.core.model.ProviderKind
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest

class WeftKeyVaultRepositoryTest : BehaviorSpec({

    Given("a WeftKeyVaultRepository over a fake substrate key vault") {

        When("a provider's key is saved") {
            Then("it is written under that provider's substrate alias") {
                runTest {
                    val vault = mock<KeyVault> { everySuspend { put(any(), any()) } returns Unit }
                    WeftKeyVaultRepository(vault).putApiKey(ProviderKind.Anthropic, "sk-ant")
                    verifySuspend { vault.put(WeftRuntime.ANTHROPIC_KEY_ALIAS, "sk-ant") }
                }
            }
        }

        When("a provider's key is read") {
            Then("it is read from that provider's substrate alias") {
                runTest {
                    val vault = mock<KeyVault> {
                        everySuspend { get(WeftRuntime.OPENAI_KEY_ALIAS) } returns "sk-oai"
                    }
                    WeftKeyVaultRepository(vault).getApiKey(ProviderKind.OpenAI) shouldBe "sk-oai"
                }
            }
        }

        When("a provider's key presence is checked") {
            Then("it checks that provider's substrate alias") {
                runTest {
                    val vault = mock<KeyVault> {
                        everySuspend { exists(WeftRuntime.DEEPSEEK_KEY_ALIAS) } returns true
                    }
                    WeftKeyVaultRepository(vault).hasApiKey(ProviderKind.DeepSeek) shouldBe true
                }
            }
        }
    }
})

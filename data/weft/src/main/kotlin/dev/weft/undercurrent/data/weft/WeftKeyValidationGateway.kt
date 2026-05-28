package dev.weft.undercurrent.data.weft

import ai.koog.prompt.Prompt
import ai.koog.prompt.executor.clients.anthropic.AnthropicClientSettings
import ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.clients.openrouter.OpenRouterClientSettings
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.executor.clients.openrouter.OpenRouterModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import dev.weft.android.WeftRuntime
import dev.weft.contracts.WeftCredentialProvider
import dev.weft.undercurrent.core.model.ProviderKind
import dev.weft.undercurrent.shared.gateway.KeyValidationGateway
import dev.weft.undercurrent.shared.gateway.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import dev.weft.contracts.ProviderKind as WeftProviderKind

/**
 * Android impl of [KeyValidationGateway]. Sends a 1-token "ping"
 * through Koog's client for the chosen [ProviderKind].
 *
 * Always runs on [Dispatchers.IO] internally — safe to call from
 * any coroutine context.
 *
 * Same explicit Ktor factory pattern WeftRuntime uses — ServiceLoader-
 * based discovery is unreliable on Android (R8 / AGP strip
 * META-INF/services entries).
 */
@OptIn(kotlin.time.ExperimentalTime::class)
public class WeftKeyValidationGateway : KeyValidationGateway {

    override suspend fun validateKey(
        provider: ProviderKind,
        apiKey: String,
    ): ValidationResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext ValidationResult.Invalid("Empty key.")
        try {
            val executor: MultiLLMPromptExecutor
            val model: LLModel
            when (provider) {
                ProviderKind.Anthropic -> {
                    val client = AnthropicLLMClient(
                        apiKey = apiKey,
                        settings = AnthropicClientSettings(
                            modelVersionsMap = mapOf(WeftRuntime.SONNET_4_6_MODEL to "claude-sonnet-4-6"),
                        ),
                        httpClientFactory = ai.koog.http.client.ktor.KtorKoogHttpClient.Factory(),
                    )
                    executor = MultiLLMPromptExecutor(client)
                    model = WeftRuntime.SONNET_4_6_MODEL
                }
                ProviderKind.OpenAI -> {
                    val client = OpenAILLMClient(
                        apiKey = apiKey,
                        settings = OpenAIClientSettings(
                            baseUrl = WeftCredentialProvider.OPENAI_BASE_URL,
                        ),
                        httpClientFactory = ai.koog.http.client.ktor.KtorKoogHttpClient.Factory(),
                    )
                    executor = MultiLLMPromptExecutor(client)
                    model = OpenAIModels.Chat.GPT4oMini
                }
                ProviderKind.OpenRouter -> {
                    val client = OpenRouterLLMClient(
                        apiKey = apiKey,
                        settings = OpenRouterClientSettings(),
                        httpClientFactory = ai.koog.http.client.ktor.KtorKoogHttpClient.Factory(),
                    )
                    executor = MultiLLMPromptExecutor(client)
                    model = OpenRouterModels.GPT4oMini
                }
                ProviderKind.DeepSeek -> {
                    val client = OpenAILLMClient(
                        apiKey = apiKey,
                        settings = OpenAIClientSettings(
                            baseUrl = WeftRuntime.DEEPSEEK_BASE_URL,
                        ),
                        httpClientFactory = ai.koog.http.client.ktor.KtorKoogHttpClient.Factory(),
                    )
                    executor = MultiLLMPromptExecutor(
                        mapOf(ai.koog.prompt.llm.LLMProvider.DeepSeek to client),
                    )
                    model = WeftRuntime.DEEPSEEK_CHAT_MODEL
                }
            }
            val response = withTimeoutOrNull(VALIDATION_TIMEOUT) {
                executor.execute(
                    prompt = Prompt(
                        messages = listOf(
                            Message.User(
                                content = "ping",
                                metaInfo = RequestMetaInfo(timestamp = Clock.System.now()),
                            ),
                        ),
                        id = "validate",
                    ),
                    model = model,
                    tools = emptyList(),
                )
            }
            if (response == null) {
                ValidationResult.Invalid("Timed out reaching ${provider.label()}.")
            } else {
                ValidationResult.Ok
            }
        } catch (t: Throwable) {
            val msg = t.message.orEmpty()
            when {
                "401" in msg || "403" in msg || "authentication" in msg.lowercase() ||
                    "incorrect api key" in msg.lowercase() || "invalid_api_key" in msg.lowercase() ->
                    ValidationResult.Invalid("Invalid API key.")
                else -> ValidationResult.Invalid("Couldn't reach ${provider.label()}: $msg")
            }
        }
    }

    private fun ProviderKind.label(): String = when (this) {
        ProviderKind.Anthropic -> "Anthropic"
        ProviderKind.OpenAI -> "OpenAI"
        ProviderKind.OpenRouter -> "OpenRouter"
        ProviderKind.DeepSeek -> "DeepSeek"
    }

    private companion object {
        private val VALIDATION_TIMEOUT = 10.seconds
    }
}

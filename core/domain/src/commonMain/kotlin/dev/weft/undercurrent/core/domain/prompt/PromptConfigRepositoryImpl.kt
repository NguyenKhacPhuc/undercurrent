package dev.weft.undercurrent.core.domain.prompt

import dev.weft.undercurrent.core.domain.SessionTokenStore
import dev.weft.undercurrent.core.model.PromptConfig
import dev.weft.undercurrent.data.network.common.BaseResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Ktor-backed [PromptConfigRepository]. Fetches `GET /v1/prompt-config` with
 * the stored session bearer (the BE wraps it in [BaseResponse]), persists a
 * successful result via [cache], and exposes the cached value as [current].
 * A failed fetch is swallowed to `null` so a previously fetched prompt is
 * never lost. I/O runs on [ioDispatcher].
 */
class PromptConfigRepositoryImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val sessionTokenStore: SessionTokenStore,
    private val cache: PromptConfigCache,
    private val ioDispatcher: CoroutineDispatcher,
) : PromptConfigRepository {

    override val current: Flow<PromptConfig?> = cache.cached

    override suspend fun refresh(): PromptConfig? = withContext(ioDispatcher) {
        val token = sessionTokenStore.read() ?: return@withContext null
        val fetched = runCatching {
            httpClient.get("$baseUrl/v1/prompt-config") { bearerAuth(token) }
                .body<BaseResponse<PromptConfig>>()
                .data
        }.getOrNull() ?: return@withContext null
        cache.save(fetched)
        fetched
    }
}

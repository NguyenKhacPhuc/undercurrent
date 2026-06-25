package dev.weft.undercurrent.core.domain.prompt

import dev.weft.undercurrent.core.model.PromptConfig
import kotlinx.coroutines.flow.Flow

/**
 * Provides the backend-driven base prompt the assistant runs on: fetch it
 * from the BE, remember the last one fetched, and expose "the prompt to use
 * now". Per the no-fallback decision, [current] is `null` until a prompt has
 * been successfully fetched — a distinct "not ready" state callers block on,
 * never an empty string or a built-in default.
 */
interface PromptConfigRepository {

    /** The prompt to use now; `null` = never successfully fetched (not ready). */
    val current: Flow<PromptConfig?>

    /**
     * Fetch the current prompt from the BE using the signed-in session. On
     * success it is cached and becomes [current]; on failure (offline, server
     * error, no session) [current] is left intact. Returns the freshly fetched
     * config, or `null` if the fetch did not succeed.
     */
    suspend fun refresh(): PromptConfig?
}

/**
 * Local persistence for the last successfully fetched [PromptConfig] —
 * survives restarts and backs offline use. [cached] is `null` when nothing
 * has been stored yet.
 */
interface PromptConfigCache {
    val cached: Flow<PromptConfig?>
    suspend fun save(config: PromptConfig)
}

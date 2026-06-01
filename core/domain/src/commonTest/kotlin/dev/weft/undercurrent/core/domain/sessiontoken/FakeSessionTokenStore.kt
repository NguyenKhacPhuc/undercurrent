package dev.weft.undercurrent.core.domain.sessiontoken

import dev.weft.undercurrent.core.domain.SessionTokenStore

/**
 * In-memory [SessionTokenStore] for tests. Single-slot, holds the token
 * in process memory only — no persistence, no encryption. **Not for
 * production use.**
 */
class FakeSessionTokenStore : SessionTokenStore {
    private var token: String? = null

    override suspend fun save(token: String) {
        this.token = token
    }

    override suspend fun read(): String? = token

    override suspend fun clear() {
        token = null
    }
}

package dev.weft.undercurrent.feature.signin

import dev.weft.undercurrent.core.domain.SessionTokenStore

/**
 * In-memory single-slot [SessionTokenStore] for `SignInViewModel` tests.
 * Mirrors the contract behavior verified by
 * `SessionTokenStoreContractSpec` in `core/domain/commonTest`. Records
 * the saved value so collaborator-interaction assertions can pin down
 * what got persisted.
 */
class FakeSessionTokenStore(initial: String? = null) : SessionTokenStore {

    var saved: String? = initial
        private set

    var clearCount: Int = 0
        private set

    override suspend fun save(token: String) {
        saved = token
    }

    override suspend fun read(): String? = saved

    override suspend fun clear() {
        saved = null
        clearCount++
    }
}

package dev.weft.undercurrent.core.domain.sessiontoken

/**
 * Runs the shared [SessionTokenStoreContractSpec] against the in-memory
 * [FakeSessionTokenStore]. Future platform impl stories
 * (mobile-auth-wiring/02, mobile-auth-wiring/03) subclass the same
 * contract spec with their real factories.
 */
class FakeSessionTokenStoreTest : SessionTokenStoreContractSpec({ FakeSessionTokenStore() })

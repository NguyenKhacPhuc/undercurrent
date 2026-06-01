package dev.weft.undercurrent.feature.signin

import dev.weft.undercurrent.core.domain.AuthRepository
import dev.weft.undercurrent.core.domain.auth.dto.AuthResponse
import dev.weft.undercurrent.core.domain.auth.dto.MeResponse
import dev.weft.undercurrent.core.ext.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Hand-rolled in-memory fake for [AuthRepository]. Each method emits
 * a programmable sequence (`Loading` followed by either `Success(T)`
 * or `Error(Throwable)`) so individual tests can stage exactly the
 * shape they're asserting against.
 *
 * Defaults all four methods to `emptyFlow()` — tests that don't touch
 * a method don't need to seed it. Methods record the args they were
 * called with so collaborator-interaction tests (later in this
 * spec) can verify the right BE endpoint was hit.
 *
 * Hand-rolled (not MockK) because this is `commonTest` — MockK is
 * JVM-only and would break the iOS build per `undercurrent/CLAUDE.md`.
 */
class FakeAuthRepository : AuthRepository {

    /** Per-method emission queue. Drained in FIFO order on each call. */
    val signUpResponses: ArrayDeque<Result<AuthResponse>> = ArrayDeque()
    val signInResponses: ArrayDeque<Result<AuthResponse>> = ArrayDeque()
    val getMeResponses: ArrayDeque<Result<MeResponse>> = ArrayDeque()
    val signOutResponses: ArrayDeque<Result<Unit>> = ArrayDeque()

    /** Call recorders. */
    data class SignUpCall(val displayName: String, val email: String, val password: String)
    data class SignInCall(val email: String, val password: String)

    val signUpCalls: MutableList<SignUpCall> = mutableListOf()
    val signInCalls: MutableList<SignInCall> = mutableListOf()
    var getMeCallCount: Int = 0
    var signOutCallCount: Int = 0

    override fun signUp(
        displayName: String,
        email: String,
        password: String,
    ): Flow<Result<AuthResponse>> {
        signUpCalls += SignUpCall(displayName, email, password)
        val next = signUpResponses.removeFirstOrNull() ?: return flow { emit(Result.Loading) }
        return flow {
            emit(Result.Loading)
            emit(next)
        }
    }

    override fun signIn(email: String, password: String): Flow<Result<AuthResponse>> {
        signInCalls += SignInCall(email, password)
        val next = signInResponses.removeFirstOrNull() ?: return flow { emit(Result.Loading) }
        return flow {
            emit(Result.Loading)
            emit(next)
        }
    }

    override fun getMe(): Flow<Result<MeResponse>> {
        getMeCallCount++
        val next = getMeResponses.removeFirstOrNull() ?: return flow { emit(Result.Loading) }
        return flow {
            emit(Result.Loading)
            emit(next)
        }
    }

    override fun signOut(): Flow<Result<Unit>> {
        signOutCallCount++
        val next = signOutResponses.removeFirstOrNull() ?: return flow { emit(Result.Loading) }
        return flow {
            emit(Result.Loading)
            emit(next)
        }
    }
}

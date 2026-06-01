package dev.weft.undercurrent.data.network.common

import io.ktor.client.network.sockets.ConnectTimeoutException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Wraps a suspending API call returning [BaseResponse] into a [Flow]
 * that emits `data` (or [Unit] when the type is `Unit`) and re-throws
 * any error — with [ConnectTimeoutException] coerced to the higher-
 * level [NetworkException] so UI just renders "no connection".
 *
 * Use when the response is the *envelope* shape `{ success, data, … }`.
 * Endpoints that return raw payloads don't need the wrapper.
 */
@Suppress("UNCHECKED_CAST")
fun <T : Any?> safeApiCall(call: suspend () -> BaseResponse<T>): Flow<T> = flow {
    try {
        val response = call()
        emit(response.data ?: Unit as T)
    } catch (e: Exception) {
        if (e is ConnectTimeoutException) {
            throw NetworkException()
        }
        throw e
    }
}

/**
 * Same as [safeApiCall] but preserves a nullable `data` field —
 * useful for "fetch optional config" endpoints where `null` is a
 * meaningful response, not a missing one.
 */
fun <T : Any?> safeApiCallNullableData(
    call: suspend () -> BaseResponse<T?>,
): Flow<T?> = flow {
    try {
        val response = call()
        emit(response.data)
    } catch (e: Exception) {
        if (e is ConnectTimeoutException) {
            throw NetworkException()
        }
        throw e
    }
}

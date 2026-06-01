package dev.weft.undercurrent.core.ext

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Generic loading wrapper for `Flow<T>` consumers. Use when a UI / view-model
 * surface wants to render distinct loading / loaded / error states from a
 * single observable source.
 *
 * Three states:
 *  - [Loading] — emitted before the underlying flow produces anything
 *  - [Success] — wraps each emitted value
 *  - [Error] — emitted if the upstream throws (the upstream completes after)
 *
 * Distinct from `kotlin.Result` (success/failure only, no Loading, shadows
 * on import). Reach for that one when you want exception-free call-site
 * error handling instead of a Flow surface.
 */
sealed interface Result<out T> {

    data class Success<T>(val data: T) : Result<T>

    data class Error(val exception: Throwable) : Result<Nothing>

    data object Loading : Result<Nothing>
}

/**
 * Wrap each emission of this Flow as a [Result.Success]; emit
 * [Result.Loading] at the start; emit [Result.Error] if the upstream
 * throws (the upstream then completes — downstream collectors see
 * `awaitComplete` after the Error item).
 *
 * The caught exception is printed before being wrapped so a crash in
 * dev / debug surfaces visibly even when the consumer is just rendering
 * the [Result.Error] state.
 */
fun <T> Flow<T>.asResult(): Flow<Result<T>> =
    this
        .map<T, Result<T>> { Result.Success(it) }
        .onStart { emit(Result.Loading) }
        .catch {
            it.printStackTrace()
            emit(Result.Error(it))
        }

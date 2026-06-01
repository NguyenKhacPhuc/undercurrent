package dev.weft.undercurrent.core.ext

import dev.weft.undercurrent.core.domain.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

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

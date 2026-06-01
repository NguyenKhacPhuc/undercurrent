package dev.weft.undercurrent.core.domain

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
 * Lives in `core/domain` (rather than `core/ext` where it was originally
 * added in PR #5) so domain interfaces — e.g. `AuthClient` — can return
 * `Flow<Result<T>>` without inverting the existing `core/ext → core/domain`
 * dependency. The Flow extension `asResult()` stays in `core/ext`.
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

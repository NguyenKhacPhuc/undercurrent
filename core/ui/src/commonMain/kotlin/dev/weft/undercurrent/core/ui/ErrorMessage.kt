package dev.weft.undercurrent.core.ui

import dev.weft.undercurrent.data.network.common.ApiException
import dev.weft.undercurrent.data.network.common.HttpException
import dev.weft.undercurrent.data.network.common.NetworkException

/**
 * Generic user-facing string for the project's common exception types.
 * Returns `null` for shapes the caller wants to map itself (e.g. a
 * feature treating 401 as "Invalid credentials" instead of the BE's
 * raw message). Feature VMs check feature-specific cases first, then
 * fall back to this helper.
 */
fun Throwable.toUserMessage(): String? = when (this) {
    is NetworkException -> "Couldn't reach the server. Check your connection."
    is ApiException -> apiMessage
    is HttpException -> "Server error ($httpStatus)."
    else -> message
}

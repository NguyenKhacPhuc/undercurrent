package dev.weft.undercurrent.core.ui

import dev.weft.undercurrent.data.network.common.ApiException
import dev.weft.undercurrent.data.network.common.HttpException
import dev.weft.undercurrent.data.network.common.NetworkException

fun Throwable.toUserMessage(): String? = when (this) {
    is NetworkException -> "Couldn't reach the server. Check your connection."
    is ApiException -> apiMessage
    is HttpException -> "Server error ($httpStatus)."
    else -> message
}

package dev.weft.undercurrent.data.network.common

object ErrorCodes {
    const val INVALID_REQUEST: String = "invalid_request"
    const val UNAUTHENTICATED: String = "unauthenticated"
    const val EMAIL_ALREADY_REGISTERED: String = "email_already_registered"
    const val RATE_LIMITED: String = "rate_limited"
}

object HttpStatus {
    const val BAD_REQUEST: Int = 400
    const val UNAUTHORIZED: Int = 401
    const val CONFLICT: Int = 409
    const val TOO_MANY_REQUESTS: Int = 429
}

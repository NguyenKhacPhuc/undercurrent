package dev.weft.undercurrent.data.network.interceptor

/**
 * Hook called when [TokenManager] detects an unrecoverable auth state
 * (refresh token also rejected, or 403 from the server). Android impl
 * relaunches MainActivity into the login flow; iOS impl is typically
 * a no-op (the foreground store reacts to a cleared TokenStore).
 */
fun interface AppRestarter {
    fun restart()
}

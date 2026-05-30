package dev.weft.undercurrent.data.network.common

/**
 * Canonical name constants for headers the app reads + writes. Constants
 * live in a single object so a typo at any call-site is a compile error.
 */
object CustomHeader {
    const val ACCESS_TOKEN: String = "access-auth-token"
    const val PLATFORM: String = "platform"
    const val TIME_ZONE: String = "timezone"
    const val LANGUAGE: String = "language"
    const val DEVICE_ID: String = "x-device-id"
    const val APP_VERSION: String = "app-version"
}

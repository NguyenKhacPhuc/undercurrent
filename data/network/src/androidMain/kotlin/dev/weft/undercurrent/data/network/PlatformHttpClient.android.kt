package dev.weft.undercurrent.data.network

import java.util.TimeZone

actual fun currentTimeZoneId(): String = TimeZone.getDefault().id

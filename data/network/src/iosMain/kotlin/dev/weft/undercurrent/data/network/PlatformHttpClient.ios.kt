package dev.weft.undercurrent.data.network

import platform.Foundation.NSTimeZone
import platform.Foundation.localTimeZone

actual fun currentTimeZoneId(): String = NSTimeZone.localTimeZone.name

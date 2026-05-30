package dev.weft.undercurrent.data.network.interceptor

import android.os.SystemClock

actual object ElapsedRealtime {
    actual fun millis(): Long = SystemClock.elapsedRealtime()
}

package dev.weft.undercurrent.data.network.interceptor

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.CLOCK_MONOTONIC
import platform.posix.clock_gettime
import platform.posix.timespec

actual object ElapsedRealtime {

    @OptIn(ExperimentalForeignApi::class)
    actual fun millis(): Long = memScoped {
        val ts = alloc<timespec>()
        clock_gettime(CLOCK_MONOTONIC.toUInt(), ts.ptr)
        ts.tv_sec * 1_000L + ts.tv_nsec / 1_000_000L
    }
}

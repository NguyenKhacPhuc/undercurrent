package dev.weft.undercurrent.core.ext

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Platform-supplied I/O dispatcher for blocking work (network, disk).
 *
 * Use from `commonMain` instead of `Dispatchers.IO` directly —
 * `Dispatchers.IO` is `internal` from common code on Apple native
 * targets in `kotlinx-coroutines` 1.9.x. Resolves to:
 *
 *  - Android / JVM → `Dispatchers.IO`
 *  - iOS / Apple   → `Dispatchers.Default` (no separate I/O pool on
 *    Apple targets; engines like Ktor's Darwin run I/O on their own
 *    queues regardless of the dispatcher passed to `flowOn`)
 */
expect val ioDispatcher: CoroutineDispatcher

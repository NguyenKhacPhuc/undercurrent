package dev.weft.undercurrent.core.domain.auth

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val authIoDispatcher: CoroutineDispatcher = Dispatchers.IO

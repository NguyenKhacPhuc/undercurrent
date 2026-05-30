package dev.weft.undercurrent.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

@Immutable
class PlatformAdapter(
    val chatRoute: @Composable () -> Unit,
    val renderedTreeRoute: @Composable () -> Unit,
    val miniAppsRoute: @Composable () -> Unit,
    val creatorRoute: @Composable () -> Unit,
    val onOpenUrl: (String) -> Unit,
    val onRestartProcess: () -> Unit,
    val onOpenAppDetailsSettings: () -> Unit
)

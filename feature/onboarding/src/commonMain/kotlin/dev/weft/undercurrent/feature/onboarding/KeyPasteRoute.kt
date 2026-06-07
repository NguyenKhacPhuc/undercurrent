package dev.weft.undercurrent.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.koinInject

@Composable
fun KeyPasteRoute(onOpenConsole: (String) -> Unit) {
    val vm: KeyPasteViewModel = koinInject()
    val state by vm.state.collectAsState()
    KeyPasteScreen(
        provider = state.provider,
        status = state.status,
        onSubmitKey = { key -> vm.dispatch(KeyPasteIntent.SubmitKey(key)) },
        onOpenConsole = onOpenConsole,
    )
}

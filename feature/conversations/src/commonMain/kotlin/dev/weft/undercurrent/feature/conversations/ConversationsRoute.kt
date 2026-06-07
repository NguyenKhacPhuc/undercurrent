package dev.weft.undercurrent.feature.conversations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.feature.chat.ChatIntent
import dev.weft.undercurrent.feature.chat.ChatViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
public fun ConversationsRoute() {
    val nav: NavigationViewModel = koinInject()
    val chat: ChatViewModel = koinInject()
    val vm: ConversationsViewModel = koinViewModel()
    val chatState by chat.state.collectAsState()
    val state by vm.state.collectAsState()
    if (!chatState.agentReady) {
        LaunchedEffect(Unit) {
            nav.dispatch(NavigationIntent.Navigate(Screen.KeyPaste))
        }
        return
    }
    ConversationsListScreen(
        state = state,
        activeConversationId = chatState.currentConversationId,
        onSelect = { id -> chat.dispatch(ChatIntent.SelectConversation(id)) },
        onNewChat = { chat.dispatch(ChatIntent.NewChat) },
        onBack = { nav.dispatch(NavigationIntent.Navigate(Screen.Chat)) },
        onQueryChange = { vm.dispatch(ConversationsIntent.SetQuery(it)) },
        onDelete = { vm.dispatch(ConversationsIntent.Delete(it)) },
        onClearAll = { vm.dispatch(ConversationsIntent.ClearAll) },
    )
}

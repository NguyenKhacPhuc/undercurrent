package dev.weft.undercurrent.feature.conversations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.Navigator
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.feature.chat.ChatIntent
import dev.weft.undercurrent.feature.chat.ChatViewModel
import org.koin.compose.koinInject

@Composable
public fun ConversationsRoute() {
    val nav: Navigator = koinInject()
    val chat: ChatViewModel = koinInject()
    val chatState by chat.state.collectAsState()
    if (!chatState.agentReady) {
        LaunchedEffect(Unit) {
            nav.dispatch(NavigationIntent.Navigate(Screen.KeyPaste))
        }
        return
    }
    ConversationsListScreen(
        activeConversationId = chatState.currentConversationId,
        onSelect = { id -> chat.dispatch(ChatIntent.SelectConversation(id)) },
        onNewChat = { chat.dispatch(ChatIntent.NewChat) },
        onBack = { nav.dispatch(NavigationIntent.Navigate(Screen.Chat)) },
    )
}

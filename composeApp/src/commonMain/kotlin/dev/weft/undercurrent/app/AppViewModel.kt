package dev.weft.undercurrent.app

import androidx.compose.runtime.snapshots.SnapshotStateList
import dev.weft.undercurrent.core.model.AppEffect
import dev.weft.undercurrent.core.model.AppState
import dev.weft.undercurrent.core.navigation.NavBackStack
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.feature.chat.components.DisplayMessage
import dev.weft.undercurrent.feature.chat.SkillSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AppViewModel {

    val state: StateFlow<AppState>

    val backStack: NavBackStack<Screen>

    val effects: Flow<AppEffect>

    val displayMessages: SnapshotStateList<DisplayMessage>

    val skills: List<SkillSummary>

    fun resume()

    fun dismissPermissionDialog()

    suspend fun sendUiEvent(
        action: String,
        sourceLabel: String?,
        fieldValues: Map<String, String>,
    )

    suspend fun saveKey(key: String)
}

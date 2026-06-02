package dev.weft.undercurrent.feature.personas

import androidx.compose.runtime.Composable
import dev.weft.undercurrent.core.model.PersonaKind
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.Navigator
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.feature.creator.CreatorIntent
import dev.weft.undercurrent.feature.creator.CreatorKind
import dev.weft.undercurrent.feature.creator.CreatorViewModel
import org.koin.compose.koinInject

@Composable
public fun PersonasRoute() {
    val nav: Navigator = koinInject()
    val creator: CreatorViewModel = koinInject()
    PersonasScreen(
        onBack = { nav.dispatch(NavigationIntent.Navigate(Screen.Chat)) },
        onStartCreator = { kind ->
            val ck = when (kind) {
                PersonaKind.Voice -> CreatorKind.PersonaVoice
                PersonaKind.Role -> CreatorKind.PersonaRole
                PersonaKind.Custom -> CreatorKind.PersonaVoice
            }
            creator.dispatch(CreatorIntent.StartCreator(ck))
        },
    )
}

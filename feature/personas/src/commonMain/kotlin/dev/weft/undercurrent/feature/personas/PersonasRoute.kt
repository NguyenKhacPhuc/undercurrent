package dev.weft.undercurrent.feature.personas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.weft.undercurrent.core.model.PersonaKind
import dev.weft.undercurrent.core.navigation.NavigationIntent
import dev.weft.undercurrent.core.navigation.NavigationViewModel
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.feature.creator.CreatorIntent
import dev.weft.undercurrent.feature.creator.CreatorKind
import dev.weft.undercurrent.feature.creator.CreatorViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
public fun PersonasRoute() {
    val nav: NavigationViewModel = koinInject()
    val creator: CreatorViewModel = koinInject()
    val vm: PersonasViewModel = koinViewModel()
    val state by vm.state.collectAsState()
    PersonasScreen(
        state = state,
        onBack = { nav.dispatch(NavigationIntent.Navigate(Screen.Chat)) },
        onStartCreator = { kind ->
            val ck = when (kind) {
                PersonaKind.Voice -> CreatorKind.PersonaVoice
                PersonaKind.Role -> CreatorKind.PersonaRole
                PersonaKind.Custom -> CreatorKind.PersonaVoice
            }
            creator.dispatch(CreatorIntent.StartCreator(ck))
        },
        onTapPersona = { vm.dispatch(PersonasIntent.TapPersona(it)) },
        onAddCustom = { name, tagline, text, kind ->
            vm.dispatch(PersonasIntent.AddCustom(name, tagline, text, kind))
        },
        onUpdateCustom = { id, name, tagline, text ->
            vm.dispatch(PersonasIntent.UpdateCustom(id, name, tagline, text))
        },
        onDeleteCustom = { id -> vm.dispatch(PersonasIntent.DeleteCustom(id)) },
    )
}

package dev.weft.undercurrent.feature.creator

import dev.weft.undercurrent.feature.creator.internal.WeftCreatorViewModel
import org.koin.dsl.module

val creatorAndroidModule = module {
    single { CreatorSession() }
    single<CreatorViewModel> {
        WeftCreatorViewModel(
            context = get(),
            agentSession = get(),
            chatVm = get(),
            creatorSession = get(),
            navigationVm = get(),
        )
    }
}

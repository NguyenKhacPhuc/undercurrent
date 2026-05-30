package dev.weft.undercurrent.feature.chat

import dev.weft.undercurrent.core.domain.ChatRepository
import dev.weft.undercurrent.feature.chat.internal.IosChatRepository
import org.koin.dsl.module

val chatIosModule = module {
    single<ChatRepository> {
        IosChatRepository(
            keyVault = get(),
            providerPrefsRepo = get(),
            personaRepo = get(),
            db = get(),
            navigationVm = get(),
        )
    }
}

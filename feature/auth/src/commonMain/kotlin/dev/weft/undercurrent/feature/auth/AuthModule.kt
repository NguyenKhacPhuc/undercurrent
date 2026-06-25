package dev.weft.undercurrent.feature.auth

import dev.weft.undercurrent.core.domain.AuthRepository
import dev.weft.undercurrent.core.domain.auth.AUTH_REPOSITORY_QUALIFIER
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val authModule = module {
    viewModel {
        SignInViewModel(
            authRepository = get<AuthRepository>(named(AUTH_REPOSITORY_QUALIFIER)),
            sessionTokenStore = get(),
        )
    }
    viewModel {
        AccountViewModel(
            authRepository = get<AuthRepository>(named(AUTH_REPOSITORY_QUALIFIER)),
            sessionTokenStore = get(),
        )
    }
    viewModel {
        PromptSetupViewModel(promptConfig = get())
    }
}

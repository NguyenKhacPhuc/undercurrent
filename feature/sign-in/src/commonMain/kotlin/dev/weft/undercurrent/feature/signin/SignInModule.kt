package dev.weft.undercurrent.feature.signin

import dev.weft.undercurrent.core.domain.AuthRepository
import dev.weft.undercurrent.core.domain.auth.AUTH_REPOSITORY_QUALIFIER
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val signInModule = module {
    viewModel {
        SignInViewModel(
            authRepository = get<AuthRepository>(named(AUTH_REPOSITORY_QUALIFIER)),
            sessionTokenStore = get(),
        )
    }
}

package dev.weft.undercurrent.feature.signin

import dev.weft.undercurrent.core.domain.AuthRepository
import dev.weft.undercurrent.core.domain.auth.AUTH_REPOSITORY_QUALIFIER
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Koin binding for the sign-in feature. Resolves the qualified
 * [AuthRepository] from `core.domain.auth.authRepositoryModule` —
 * make sure that module is loaded too (it's in `allModules` /
 * `iosAllModules` by `mobile-auth-wiring/04`).
 */
val signInModule = module {
    single {
        SignInViewModel(
            authRepository = get<AuthRepository>(named(AUTH_REPOSITORY_QUALIFIER)),
            sessionTokenStore = get(),
        )
    }
}

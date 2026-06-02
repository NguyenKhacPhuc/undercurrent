package dev.weft.undercurrent.feature.chat

import org.koin.dsl.module

/**
 * The agent-host bindings are shared now ([chatHostModule]); this keeps
 * the Android consumer's `chatAndroidModule` reference stable.
 */
val chatAndroidModule = module {
    includes(chatHostModule)
}

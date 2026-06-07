package dev.weft.undercurrent.feature.onboarding

import dev.weft.undercurrent.core.domain.KeyValidationRepository
import dev.weft.undercurrent.core.domain.KeyVaultRepository
import dev.weft.undercurrent.core.domain.ProviderPrefsRepository
import dev.weft.undercurrent.core.domain.ValidationResult
import dev.weft.undercurrent.core.model.ProviderKind
import dev.weft.undercurrent.feature.settings.providers.ProviderIntent
import dev.weft.undercurrent.feature.settings.providers.ProviderViewModel
import dev.weft.undercurrent.shared.mvi.MviViewModel

data class KeyPasteState(
    val provider: ProviderKind = ProviderKind.Anthropic,
    val status: KeyPasteStatus = KeyPasteStatus.Idle,
)

sealed interface KeyPasteStatus {
    data object Idle : KeyPasteStatus
    data object Validating : KeyPasteStatus
    data class Failed(val message: String) : KeyPasteStatus
}

sealed interface KeyPasteIntent {
    /** Validate the pasted key; on success persist it and build the agent. */
    data class SubmitKey(val key: String) : KeyPasteIntent
}

sealed interface KeyPasteEffect

class KeyPasteViewModel(
    private val validator: KeyValidationRepository,
    private val keyVault: KeyVaultRepository,
    providerPrefs: ProviderPrefsRepository,
    private val provider: ProviderViewModel,
) : MviViewModel<KeyPasteState, KeyPasteIntent, KeyPasteEffect>(
    initialState = KeyPasteState(provider = providerPrefs.activeProvider.value),
) {
    init {
        providerPrefs.activeProvider.collectInto { copy(provider = it) }
    }

    override fun dispatch(intent: KeyPasteIntent) = launch {
        when (intent) {
            is KeyPasteIntent.SubmitKey -> {
                val target = current.provider
                update { it.copy(status = KeyPasteStatus.Validating) }
                when (val result = validator.validateKey(target, intent.key)) {
                    is ValidationResult.Ok -> {
                        keyVault.putApiKey(target, intent.key)
                        provider.dispatch(ProviderIntent.SubmitKey(intent.key))
                    }
                    is ValidationResult.Invalid ->
                        update { it.copy(status = KeyPasteStatus.Failed(result.message)) }
                }
            }
        }
    }
}

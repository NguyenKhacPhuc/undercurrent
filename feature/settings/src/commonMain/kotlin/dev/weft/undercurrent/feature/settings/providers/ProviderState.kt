package dev.weft.undercurrent.feature.settings.providers

import dev.weft.undercurrent.core.domain.KeyValidationRepository
import dev.weft.undercurrent.core.domain.KeyVaultRepository
import dev.weft.undercurrent.core.domain.ModelCatalogRepository
import dev.weft.undercurrent.core.domain.ModelInfo
import dev.weft.undercurrent.core.domain.ModelPool
import dev.weft.undercurrent.core.domain.ModelPrefsRepository
import dev.weft.undercurrent.core.domain.ProviderPrefsRepository
import dev.weft.undercurrent.core.domain.Slot
import dev.weft.undercurrent.core.domain.ValidationResult
import dev.weft.undercurrent.core.model.ModelTier
import dev.weft.undercurrent.core.model.ProviderKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Per-provider model directory snapshot, surfaced to the View as state. */
data class ProviderModels(
    val models: List<ModelInfo>,
    val defaultPool: ModelPool,
)

/** Async key-validation status for the expanded provider card. */
sealed interface KeyValidationStatus {
    data object Idle : KeyValidationStatus
    data object Checking : KeyValidationStatus
    data class Invalid(val message: String) : KeyValidationStatus
}

/**
 * Everything the providers screen reads. Built from the provider/model
 * preference flows, the (static) model catalog, and key-vault presence.
 * The View only ever sees this — no repositories.
 */
data class ProviderState(
    val activeProvider: ProviderKind = ProviderKind.Anthropic,
    val defaultTier: ModelTier? = null,
    val keyStatus: Map<ProviderKind, String> = emptyMap(),
    val catalogs: Map<ProviderKind, ProviderModels> = emptyMap(),
    val overrides: Map<Slot, String> = emptyMap(),
    val keyValidation: KeyValidationStatus = KeyValidationStatus.Idle,
) {
    fun modelsFor(provider: ProviderKind): ProviderModels? = catalogs[provider]

    fun overrideFor(provider: ProviderKind, tier: ModelTier): String? =
        overrides[Slot(provider, tier)]
}

/**
 * Shared, platform-agnostic producer of [ProviderState]. Both the
 * Android (Weft-backed) and iOS provider ViewModels delegate their
 * `state` here so the reactive plumbing lives in one place; only the
 * key-mutation side effects (agent rebuild on Android, app dispatch on
 * iOS) differ.
 */
class ProviderStateStore(
    private val providerPrefs: ProviderPrefsRepository,
    modelPrefs: ModelPrefsRepository,
    catalog: ModelCatalogRepository,
    private val keyVault: KeyVaultRepository,
    private val validator: KeyValidationRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val catalogs: Map<ProviderKind, ProviderModels> =
        ProviderKind.entries.associateWith { provider ->
            ProviderModels(
                models = catalog.modelsForProvider(provider),
                defaultPool = catalog.defaultPoolForProvider(provider),
            )
        }

    private val keyStatus = MutableStateFlow<Map<ProviderKind, String>>(emptyMap())
    private val validation = MutableStateFlow<KeyValidationStatus>(KeyValidationStatus.Idle)

    val state: StateFlow<ProviderState> = combine(
        providerPrefs.activeProvider,
        providerPrefs.defaultTier,
        modelPrefs.overrides,
        keyStatus,
        validation,
    ) { active, tier, overrides, keys, valid ->
        ProviderState(
            activeProvider = active,
            defaultTier = tier,
            keyStatus = keys,
            catalogs = catalogs,
            overrides = overrides,
            keyValidation = valid,
        )
    }.stateIn(scope, SharingStarted.Eagerly, ProviderState(catalogs = catalogs))

    init {
        scope.launch { refreshKeyStatus() }
    }

    suspend fun refreshKeyStatus() {
        keyStatus.value = ProviderKind.entries.mapNotNull { provider ->
            val present = runCatching { keyVault.hasApiKey(provider) }.getOrDefault(false)
            if (present) provider to KEY_PRESENT_MARKER else null
        }.toMap()
    }

    fun clearValidation() {
        validation.value = KeyValidationStatus.Idle
    }

    fun markKeyPresent(provider: ProviderKind) {
        keyStatus.value = keyStatus.value + (provider to KEY_PRESENT_MARKER)
    }

    fun markKeyRemoved(provider: ProviderKind) {
        keyStatus.value = keyStatus.value - provider
    }

    /**
     * Validate [key] for [provider]; on success run the platform [save]
     * side effect and reflect the new key in state. On failure surface
     * the message via [KeyValidationStatus.Invalid].
     */
    fun validateAndSave(provider: ProviderKind, key: String, save: suspend () -> Unit) {
        scope.launch {
            validation.value = KeyValidationStatus.Checking
            when (val result = validator.validateKey(provider, key)) {
                is ValidationResult.Ok -> {
                    save()
                    markKeyPresent(provider)
                    validation.value = KeyValidationStatus.Idle
                }
                is ValidationResult.Invalid ->
                    validation.value = KeyValidationStatus.Invalid(result.message)
            }
        }
    }

    companion object {
        const val KEY_PRESENT_MARKER: String = "•••"
    }
}

package dev.weft.undercurrent.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.weft.undercurrent.core.model.ModelTier
import dev.weft.undercurrent.core.model.ProviderKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Per-(provider, tier) model-id override. Empty / unset for any slot
 * means "use the provider's default for that tier".
 *
 * Persistence shape: one preferences key per slot, name format
 * `<provider>__<tier>` (e.g. `Anthropic__Cheap`). Values are model
 * ids matching Weft's `LLModel.id` — resolved back to typed model
 * instances at agent-build time. Stale ids (model removed from the
 * catalog) silently fall back to the default.
 *
 * Flat keys-with-double-underscore is the cheapest viable scheme
 * for the fixed grid (4 providers × 4 tiers = 16 keys max). Switch
 * to JSON-blob storage if we ever ship nested or variable-arity
 * overrides.
 *
 * KMP — commonMain. Moved from
 * `app/.../features/providers/ModelPrefsRepository.kt`.
 */
public class ModelPrefsRepository(
    private val dataStore: DataStore<Preferences>,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val rawFlow: Flow<Map<Slot, String>> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            buildMap {
                ProviderKind.entries.forEach { provider ->
                    ModelTier.entries.forEach { tier ->
                        val slot = Slot(provider, tier)
                        val value = prefs[slot.key()]
                        if (!value.isNullOrBlank()) put(slot, value)
                    }
                }
            }
        }

    public val overrides: StateFlow<Map<Slot, String>> = rawFlow
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    /** Model id override for a specific slot, or null when unset. */
    public fun overrideFor(provider: ProviderKind, tier: ModelTier): String? =
        overrides.value[Slot(provider, tier)]

    public suspend fun setOverride(provider: ProviderKind, tier: ModelTier, modelId: String?) {
        val slot = Slot(provider, tier)
        dataStore.edit { prefs ->
            if (modelId.isNullOrBlank()) prefs.remove(slot.key())
            else prefs[slot.key()] = modelId
        }
    }

    /** One-shot snapshot for boot-paths. */
    public suspend fun snapshot(): Map<Slot, String> = rawFlow.first()

    public companion object {
        public const val FILE_NAME: String = "model_prefs"
    }
}

/** (provider, tier) pair as a single keyable value. */
public data class Slot(public val provider: ProviderKind, public val tier: ModelTier) {
    internal fun key() = stringPreferencesKey("${provider.name}__${tier.name}")
}

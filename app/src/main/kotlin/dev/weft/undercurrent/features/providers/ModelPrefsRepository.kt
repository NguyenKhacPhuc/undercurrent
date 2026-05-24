package dev.weft.undercurrent.features.providers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.weft.contracts.ProviderKind
import dev.weft.harness.agents.routing.ModelTier
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

private val Context.modelPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "model_prefs",
)

/**
 * Per-(provider, tier) model-id override. Empty / unset for any slot
 * means "use the provider's default for that tier" (as wired in
 * [dev.weft.android.WeftRuntime.buildExecutorFor]).
 *
 * Persistence shape: one preferences key per slot, name format
 * `<provider>__<tier>` (e.g. `Anthropic__Cheap`). Values are model ids
 * matching the strings on Koog's `LLModel.id` — resolved back to typed
 * [ai.koog.prompt.llm.LLModel] instances at agent-build time via
 * [dev.weft.android.routing.findModelInCatalog]. Stale ids (model
 * removed from the catalog) fall back silently to the default.
 *
 * The flat keys-with-double-underscore approach is the cheapest viable
 * scheme for the small fixed grid we have (4 providers × 4 tiers = 16
 * keys max). Switch to JSON-blob storage if we ever ship nested or
 * variable-arity overrides.
 */
internal class ModelPrefsRepository(context: Context) {
    private val dataStore = context.applicationContext.modelPrefsDataStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val rawFlow: Flow<Map<Slot, String>> = dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
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

    /**
     * Snapshot of all (provider, tier) → model-id overrides. Consumers
     * typically reach in for a specific slot via [overrideFor].
     */
    val overrides: StateFlow<Map<Slot, String>> = rawFlow
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    /** Convenience: model id override for a specific slot, or null when unset. */
    fun overrideFor(provider: ProviderKind, tier: ModelTier): String? =
        overrides.value[Slot(provider, tier)]

    suspend fun setOverride(provider: ProviderKind, tier: ModelTier, modelId: String?) {
        val slot = Slot(provider, tier)
        dataStore.edit { prefs ->
            if (modelId.isNullOrBlank()) prefs.remove(slot.key())
            else prefs[slot.key()] = modelId
        }
    }

    /**
     * One-shot snapshot for AppStore boot-paths that can't wait for the
     * StateFlow's initial value (the DataStore read is async). Returns
     * the current persisted map.
     */
    suspend fun snapshot(): Map<Slot, String> = rawFlow.first()
}

/** (provider, tier) pair as a single keyable value. */
internal data class Slot(val provider: ProviderKind, val tier: ModelTier) {
    fun key() = stringPreferencesKey("${provider.name}__${tier.name}")
}

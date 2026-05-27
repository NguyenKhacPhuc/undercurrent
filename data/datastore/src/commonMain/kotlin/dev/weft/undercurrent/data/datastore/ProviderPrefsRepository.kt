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
 * Persisted user choice of LLM provider + default model tier.
 *
 *  - **Provider** — which backend the agent talks to. Defaults to
 *    Anthropic for back-compat (the onboarding flow assumes
 *    Anthropic).
 *  - **Default tier** — which [ModelTier] the per-message picker
 *    starts at (null = "Auto" = let the router decide).
 *
 * Both values are exposed as [StateFlow] for sync access from the
 * runtime factory + per-send agent calls.
 *
 * KMP — commonMain. Moved from
 * `app/.../features/providers/ProviderPrefsRepository.kt`. Now uses
 * the [ProviderKind] + [ModelTier] mirrors from :core:model instead
 * of Weft's enums directly. The `keyAlias()` extension that mapped
 * ProviderKind to Weft's KeyVault aliases lives in :data:weft (it's
 * Weft-specific).
 */
public class ProviderPrefsRepository(
    private val dataStore: DataStore<Preferences>,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val providerFlow: Flow<ProviderKind> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            prefs[KeyProvider]?.let {
                runCatching { ProviderKind.valueOf(it) }.getOrNull()
            } ?: ProviderKind.Anthropic
        }

    private val defaultTierFlow: Flow<ModelTier?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val name = prefs[KeyDefaultTier] ?: return@map null
            if (name == TIER_AUTO_SENTINEL) null
            else runCatching { ModelTier.valueOf(name) }.getOrNull()
        }

    public val activeProvider: StateFlow<ProviderKind> = providerFlow
        .stateIn(scope, SharingStarted.Eagerly, ProviderKind.Anthropic)

    public val defaultTier: StateFlow<ModelTier?> = defaultTierFlow
        .stateIn(scope, SharingStarted.Eagerly, null)

    public suspend fun setProvider(provider: ProviderKind) {
        dataStore.edit { it[KeyProvider] = provider.name }
    }

    public suspend fun setDefaultTier(tier: ModelTier?) {
        dataStore.edit { prefs ->
            if (tier == null) prefs[KeyDefaultTier] = TIER_AUTO_SENTINEL
            else prefs[KeyDefaultTier] = tier.name
        }
    }

    /** Suspending one-shot snapshot. Used by the runtime factory at boot. */
    public suspend fun activeProviderNow(): ProviderKind = providerFlow.first()

    public companion object {
        public const val FILE_NAME: String = "provider_prefs"
        private val KeyProvider = stringPreferencesKey("provider")
        private val KeyDefaultTier = stringPreferencesKey("default_tier")
        // Sentinel so "user explicitly picked Auto" survives reads
        // distinct from "preference never set" — both resolve to null
        // at the API surface but persistence-wise we want the user
        // choice to win.
        private const val TIER_AUTO_SENTINEL: String = "__AUTO__"
    }
}

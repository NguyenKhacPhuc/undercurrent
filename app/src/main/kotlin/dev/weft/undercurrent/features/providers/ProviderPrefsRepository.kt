package dev.weft.undercurrent.features.providers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.weft.android.WeftRuntime
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

private val Context.providerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "provider_prefs",
)

/**
 * Persisted user choice of LLM provider + default model tier.
 *
 * **Provider**: which backend the agent talks to. Defaults to Anthropic
 * for back-compat (the onboarding flow assumes Anthropic too). Switching
 * to OpenAI requires a separate API key — those are stored in the
 * existing [dev.weft.contracts.KeyVault] under provider-specific aliases
 * ([WeftRuntime.ANTHROPIC_KEY_ALIAS] / [WeftRuntime.OPENAI_KEY_ALIAS]).
 *
 * **Default tier**: which [ModelTier] the per-message picker starts at
 * (`null` = "Auto" = let the router decide). Independent of provider.
 *
 * Both values are exposed as [StateFlow] for sync access from the
 * `AppStore` factory + per-send agent calls.
 */
internal class ProviderPrefsRepository(context: Context) {
    private val dataStore = context.applicationContext.providerDataStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val providerFlow: Flow<ProviderKind> = dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs ->
            prefs[KeyProvider]?.let {
                runCatching { ProviderKind.valueOf(it) }.getOrNull()
            } ?: ProviderKind.Anthropic
        }

    private val defaultTierFlow: Flow<ModelTier?> = dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs ->
            val name = prefs[KeyDefaultTier] ?: return@map null
            if (name == TIER_AUTO_SENTINEL) null
            else runCatching { ModelTier.valueOf(name) }.getOrNull()
        }

    val activeProvider: StateFlow<ProviderKind> = providerFlow
        .stateIn(scope, SharingStarted.Eagerly, ProviderKind.Anthropic)

    val defaultTier: StateFlow<ModelTier?> = defaultTierFlow
        .stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun setProvider(provider: ProviderKind) {
        dataStore.edit { it[KeyProvider] = provider.name }
    }

    suspend fun setDefaultTier(tier: ModelTier?) {
        dataStore.edit { prefs ->
            if (tier == null) prefs[KeyDefaultTier] = TIER_AUTO_SENTINEL
            else prefs[KeyDefaultTier] = tier.name
        }
    }

    /** One-shot snapshot. Used by AppStore.handleResume to avoid racing the StateFlow init. */
    suspend fun activeProviderNow(): ProviderKind = providerFlow.first()

    private companion object {
        val KeyProvider = stringPreferencesKey("provider")
        val KeyDefaultTier = stringPreferencesKey("default_tier")
        // Sentinel so we can tell "user explicitly picked Auto" apart from
        // "preference never set" — both resolve to null at the API surface
        // but persistence-wise we want the user choice to win on read.
        const val TIER_AUTO_SENTINEL = "__AUTO__"
    }
}

/** Resolve a provider to the key vault alias holding its API key. */
internal fun ProviderKind.keyAlias(): String = when (this) {
    ProviderKind.Anthropic -> WeftRuntime.ANTHROPIC_KEY_ALIAS
    ProviderKind.OpenAI -> WeftRuntime.OPENAI_KEY_ALIAS
    ProviderKind.OpenRouter -> WeftRuntime.OPENROUTER_KEY_ALIAS
    ProviderKind.DeepSeek -> WeftRuntime.DEEPSEEK_KEY_ALIAS
}

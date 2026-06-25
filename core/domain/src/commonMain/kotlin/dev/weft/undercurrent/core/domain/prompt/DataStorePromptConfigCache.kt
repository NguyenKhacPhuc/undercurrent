package dev.weft.undercurrent.core.domain.prompt

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.weft.undercurrent.core.model.PromptConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * DataStore-backed [PromptConfigCache] — persists the last successfully
 * fetched [PromptConfig] as one JSON blob so it survives restarts and backs
 * offline use. A corrupt/absent value decodes to `null` (the "not ready"
 * state). Mirrors the other DataStore-backed repositories' file conventions.
 */
class DataStorePromptConfigCache(
    private val dataStore: DataStore<Preferences>,
) : PromptConfigCache {

    private val json = Json { ignoreUnknownKeys = true }

    override val cached: Flow<PromptConfig?> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            prefs[KeyConfig]?.let {
                runCatching { json.decodeFromString(PromptConfig.serializer(), it) }.getOrNull()
            }
        }

    override suspend fun save(config: PromptConfig) {
        dataStore.edit { it[KeyConfig] = json.encodeToString(PromptConfig.serializer(), config) }
    }

    companion object {
        const val FILE_NAME: String = "prompt_config_prefs"
        private val KeyConfig = stringPreferencesKey("config")
    }
}

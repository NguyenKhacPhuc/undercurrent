package dev.weft.undercurrent.core.domain

import android.content.SharedPreferences

/**
 * Android impl of [SessionTokenStore] backed by `EncryptedSharedPreferences`.
 *
 * Takes a [SharedPreferences] directly so the encryption setup
 * (`MasterKey` + `EncryptedSharedPreferences.create`) lives in the Koin
 * binding — that keeps this class testable without a real `Context`.
 *
 * Single-slot: one [SESSION_TOKEN_KEY] entry, overwritten by [save],
 * removed by [clear].
 */
class EncryptedSharedPreferencesSessionTokenStore(
    private val prefs: SharedPreferences,
) : SessionTokenStore {

    override suspend fun save(token: String) {
        prefs.edit().putString(SESSION_TOKEN_KEY, token).apply()
    }

    override suspend fun read(): String? = prefs.getString(SESSION_TOKEN_KEY, null)

    override suspend fun clear() {
        prefs.edit().remove(SESSION_TOKEN_KEY).apply()
    }

    companion object {
        const val SESSION_TOKEN_KEY: String = "be_session_token"

        /** File name under which the encrypted prefs live. */
        const val PREFS_FILE_NAME: String = "undercurrent_secure_session"
    }
}

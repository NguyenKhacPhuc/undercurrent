package dev.weft.undercurrent.core.domain

import android.content.SharedPreferences
import dev.weft.undercurrent.core.domain.sessiontoken.SessionTokenStoreContractSpec
import io.mockk.every
import io.mockk.mockk

/**
 * Runs the shared [SessionTokenStoreContractSpec] against
 * [EncryptedSharedPreferencesSessionTokenStore] in `androidUnitTest`.
 *
 * `EncryptedSharedPreferences` itself needs a real Android `Context` to
 * construct (Keystore-backed master key), so this test does NOT exercise
 * the encryption layer — that's exercised in production. What this test
 * does cover: the impl's own save/read/clear logic on top of any
 * [SharedPreferences]. Encrypted construction lives in the Koin module
 * and is verified at runtime via the e2e DoD smoke (Inception _index.md).
 */
class EncryptedSharedPreferencesSessionTokenStoreTest : SessionTokenStoreContractSpec({
    val storage = mutableMapOf<String, String?>()
    val editor: SharedPreferences.Editor = mockk(relaxed = true) {
        every { putString(any(), any()) } answers {
            storage[firstArg()] = secondArg<String?>()
            this@mockk
        }
        every { remove(any()) } answers {
            storage.remove(firstArg())
            this@mockk
        }
    }
    val prefs: SharedPreferences = mockk(relaxed = true) {
        every { getString(any(), any()) } answers {
            storage[firstArg()] ?: secondArg<String?>()
        }
        every { edit() } returns editor
    }
    EncryptedSharedPreferencesSessionTokenStore(prefs)
})

package dev.weft.undercurrent.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * iOS factory for KMP DataStore-Preferences. Writes the file under
 * `NSDocumentDirectory` so it ends up in the app's Documents folder
 * — same place native iOS apps put per-user data; iCloud backup
 * automatically includes it.
 *
 * Use in the iosMain DI module:
 *
 * ```kotlin
 * single<DataStore<Preferences>>(named("theme")) {
 *     createPreferencesDataStore(name = ThemeRepository.FILE_NAME)
 * }
 * single { ThemeRepository(get(named("theme"))) }
 * ```
 */
@OptIn(ExperimentalForeignApi::class)
fun createPreferencesDataStore(name: String): DataStore<Preferences> =
    createPreferencesDataStore(
        producePath = {
            val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = false,
                error = null,
            )
            requireNotNull(documentDirectory) {
                "Could not resolve NSDocumentDirectory for DataStore file '$name'"
            }
            requireNotNull(documentDirectory.path) {
                "NSDocumentDirectory URL had no path component"
            } + "/$name$DATA_STORE_FILE_EXT"
        }
    )

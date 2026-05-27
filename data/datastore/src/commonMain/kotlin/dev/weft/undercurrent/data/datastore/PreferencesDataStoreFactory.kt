package dev.weft.undercurrent.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * KMP factory for `DataStore<Preferences>`. Caller supplies a function
 * producing the absolute file path (different per platform):
 *
 *   - Android: `context.filesDir.resolve("<name>.preferences_pb").absolutePath`
 *   - iOS: `<NSDocumentDirectory>/<name>.preferences_pb`
 *   - JVM (tests / desktop): any writable file path
 *
 * Two convenience helpers wrap this in `:data:datastore`:
 *   - [createPreferencesDataStore] (Android variant — takes a Context)
 *   - [createPreferencesDataStore] (iOS variant — uses NSDocumentDirectory)
 *
 * Both call into this common factory; the platform-specific code is a
 * thin shim that just builds the path.
 *
 * Why okio: DataStore commonMain operates on `okio.Path`. The platform
 * factories convert their native path (Java File / NSString) to an
 * `okio.Path` before handing it back.
 */
public fun createPreferencesDataStore(
    producePath: () -> String,
): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = androidx.datastore.preferences.core.PreferencesSerializer,
            producePath = { producePath().toPath() },
        ),
    )

/**
 * Standard DataStore filename suffix. DataStore-Preferences serializes
 * via Protobuf so the on-disk format is the same regardless of platform —
 * keep the same extension everywhere so tools recognize it.
 */
public const val DATA_STORE_FILE_EXT: String = ".preferences_pb"

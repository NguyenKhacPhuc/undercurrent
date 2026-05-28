package dev.weft.undercurrent.data.sqldelight

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.weft.undercurrent.db.UndercurrentDatabase

/**
 * Android driver factory. Wraps [AndroidSqliteDriver] which uses the
 * framework SupportSQLiteOpenHelper.
 *
 * Note on bundled SQLite: Weft's own database wires Osmerion's
 * bundled SQLite to guarantee FTS5 across all minSdks. Undercurrent's
 * own `Records` schema doesn't use FTS5 (single-table generic store
 * — filters happen in Kotlin), so we use the system SQLite here.
 * If a future schema needs FTS5, swap the openHelper factory to
 * `OsmerionSQLiteOpenHelperFactory` — same trick the Weft SDK uses.
 */
actual class DatabaseDriverFactory(
    private val context: Context,
) {
    actual fun create(): SqlDriver = AndroidSqliteDriver(
        schema = UndercurrentDatabase.Schema,
        context = context.applicationContext,
        name = DATABASE_NAME,
    )
}

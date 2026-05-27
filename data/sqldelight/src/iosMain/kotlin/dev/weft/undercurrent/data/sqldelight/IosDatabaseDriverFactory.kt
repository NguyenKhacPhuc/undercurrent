package dev.weft.undercurrent.data.sqldelight

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.weft.undercurrent.db.UndercurrentDatabase

/**
 * iOS driver factory. Uses [NativeSqliteDriver] which wraps the
 * system SQLite via Kotlin/Native interop. Modern iOS ships SQLite
 * 3.x with FTS5 + window functions, so we don't need a bundled
 * variant the way Android sometimes does.
 *
 * Database file lands under `NSDocumentDirectory` (driver default
 * for the `name` constructor) so it's iCloud-backupable.
 */
public actual class DatabaseDriverFactory {
    public actual fun create(): SqlDriver = NativeSqliteDriver(
        schema = UndercurrentDatabase.Schema,
        name = DATABASE_NAME,
    )
}

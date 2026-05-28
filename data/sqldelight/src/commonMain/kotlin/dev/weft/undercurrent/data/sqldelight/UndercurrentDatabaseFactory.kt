package dev.weft.undercurrent.data.sqldelight

import app.cash.sqldelight.db.SqlDriver
import dev.weft.undercurrent.db.UndercurrentDatabase

/**
 * Builds the SqlDelight driver for `UndercurrentDatabase`. Different
 * implementations per platform — Android needs a `Context`, iOS just
 * needs the schema. The DI layer constructs this once at app startup
 * and injects it; consumer code calls [create] each time it needs a
 * driver (typically just once, at database construction).
 *
 * KMP — expect/actual. See androidMain + iosMain for implementations.
 */
expect class DatabaseDriverFactory {
    fun create(): SqlDriver
}

/**
 * Build `UndercurrentDatabase` from an already-constructed driver.
 * Convenience helper so consumers don't need to import the
 * SqlDelight generated class.
 *
 * Typical wiring:
 * ```kotlin
 * single { createUndercurrentDatabase(get<DatabaseDriverFactory>().create()) }
 * ```
 */
fun createUndercurrentDatabase(driver: SqlDriver): UndercurrentDatabase =
    UndercurrentDatabase(driver = driver)

/**
 * The on-disk database filename shared across both platforms. SQLite
 * doesn't care about the suffix — we use `.db` for tooling
 * recognition.
 */
const val DATABASE_NAME: String = "undercurrent.db"

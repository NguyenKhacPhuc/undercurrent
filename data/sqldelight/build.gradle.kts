// KMP-targeted SQLDelight module. The schema lives in
// src/commonMain/sqldelight/dev/weft/undercurrent/db/*.sq, and the
// generated database type is consumable from commonMain. Platform-
// specific drivers wire in androidMain (AndroidSqliteDriver) and
// iosMain (NativeSqliteDriver).

plugins {
    alias(libs.plugins.undercurrent.kmp.library)
    alias(libs.plugins.sqldelight)
}

sqldelight {
    databases {
        create("UndercurrentDatabase") {
            packageName.set("dev.weft.undercurrent.db")
            // Schema snapshots written here when `generateUndercurrentDatabaseSchema`
            // runs; check them in alongside any .sqm migration so
            // `verifyMigrations` proves data survives upgrade paths.
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            verifyMigrations.set(true)
        }
    }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines.extensions)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
            // Bundled SQLite for Android — same FTS5 guarantee as Weft.
            implementation(libs.osmerion.sqlite.android)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.data.sqldelight"
}

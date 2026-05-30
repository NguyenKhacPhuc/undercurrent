package dev.weft.undercurrent.data.sqldelight

import dev.weft.undercurrent.db.UndercurrentDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseAndroidModule = module {
    single { DatabaseDriverFactory(androidContext()) }
    single<UndercurrentDatabase> {
        createUndercurrentDatabase(get<DatabaseDriverFactory>().create())
    }
}

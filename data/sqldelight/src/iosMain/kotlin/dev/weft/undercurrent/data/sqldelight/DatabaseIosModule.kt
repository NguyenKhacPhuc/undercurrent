package dev.weft.undercurrent.data.sqldelight

import dev.weft.undercurrent.db.UndercurrentDatabase
import org.koin.dsl.module

val databaseIosModule = module {
    single { DatabaseDriverFactory() }
    single<UndercurrentDatabase> {
        createUndercurrentDatabase(get<DatabaseDriverFactory>().create())
    }
}

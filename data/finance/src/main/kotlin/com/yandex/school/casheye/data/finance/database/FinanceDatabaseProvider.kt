package com.yandex.school.casheye.data.finance.database

import android.content.Context
import androidx.room.Room
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/** Creates the app-scoped Room database and owns its migration registration. */
@Inject
@SingleIn(AppScope::class)
class FinanceDatabaseProvider(
    context: Context,
) {
    internal val database: FinanceDatabase =
        Room.databaseBuilder(context.applicationContext, FinanceDatabase::class.java, DATABASE_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    private companion object {
        const val DATABASE_NAME = "finance.db"
    }
}

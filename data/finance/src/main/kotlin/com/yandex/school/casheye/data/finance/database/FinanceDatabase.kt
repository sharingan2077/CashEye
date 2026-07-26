package com.yandex.school.casheye.data.finance.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yandex.school.casheye.data.finance.database.dao.AccountDao
import com.yandex.school.casheye.data.finance.database.dao.CategoryDao
import com.yandex.school.casheye.data.finance.database.dao.ExchangeRateDao
import com.yandex.school.casheye.data.finance.database.dao.OfflineWriteDao
import com.yandex.school.casheye.data.finance.database.dao.PendingOperationDao
import com.yandex.school.casheye.data.finance.database.dao.TransactionDao
import com.yandex.school.casheye.data.finance.database.entity.AccountEntity
import com.yandex.school.casheye.data.finance.database.entity.CategoryEntity
import com.yandex.school.casheye.data.finance.database.entity.ExchangeRateCoverageEntity
import com.yandex.school.casheye.data.finance.database.entity.ExchangeRateEntity
import com.yandex.school.casheye.data.finance.database.entity.PendingOperationEntity
import com.yandex.school.casheye.data.finance.database.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        PendingOperationEntity::class,
        ExchangeRateEntity::class,
        ExchangeRateCoverageEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(FinanceConverters::class)
internal abstract class FinanceDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao

    abstract fun categoryDao(): CategoryDao

    abstract fun transactionDao(): TransactionDao

    abstract fun pendingOperationDao(): PendingOperationDao

    abstract fun offlineWriteDao(): OfflineWriteDao

    abstract fun exchangeRateDao(): ExchangeRateDao
}

internal val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN currency TEXT NOT NULL DEFAULT 'RUB'")
            db.execSQL(
                """
                UPDATE transactions
                SET currency = COALESCE(
                    (SELECT accounts.currency FROM accounts WHERE accounts.id = transactions.account_id),
                    'RUB'
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS exchange_rates (
                    base_currency TEXT NOT NULL,
                    quote_currency TEXT NOT NULL,
                    rate TEXT NOT NULL,
                    rate_date TEXT NOT NULL,
                    fetched_at INTEGER NOT NULL,
                    PRIMARY KEY(base_currency, quote_currency, rate_date)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_exchange_rates_quote_currency ON exchange_rates(quote_currency)",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_exchange_rates_rate_date ON exchange_rates(rate_date)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_exchange_rates_fetched_at ON exchange_rates(fetched_at)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS exchange_rate_coverages (
                    start_date TEXT NOT NULL,
                    end_date TEXT NOT NULL,
                    fetched_at INTEGER NOT NULL,
                    PRIMARY KEY(start_date, end_date)
                )
                """.trimIndent(),
            )
        }
    }

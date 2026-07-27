package com.yandex.school.casheye.data.finance.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinanceDatabaseMigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            FinanceDatabase::class.java,
        )

    @Test
    fun migrationOneToTwoPreservesOfflineDataAndBackfillsTransactionCurrency() {
        val database =
            helper.createDatabase(DATABASE_NAME, 1).apply {
                execSQL("INSERT INTO accounts VALUES (1, 'Main', 'card', '100.00', 'USD')")
                execSQL("INSERT INTO categories VALUES (2, 'Food', 'food', 0)")
                execSQL(
                    """
                    INSERT INTO transactions
                    VALUES (3, 1, 2, '12.00', 1000, NULL, 1000, 1000)
                    """.trimIndent(),
                )
                execSQL(
                    """
                    INSERT INTO pending_operations
                    (id, entity_type, operation_type, local_entity_id, related_account_id,
                     depends_on_operation_id, created_at, payload)
                    VALUES (4, 'TRANSACTION', 'UPDATE', 3, 1, NULL, 1000, '{}')
                    """.trimIndent(),
                )
            }

        MIGRATION_1_2.migrate(database)

        database.query("SELECT currency FROM transactions WHERE id = 3").use { cursor ->
            cursor.moveToFirst()
            assertEquals("USD", cursor.getString(0))
        }
        database.query("SELECT COUNT(*) FROM pending_operations").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        database.query("SELECT COUNT(*) FROM exchange_rates").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        database.close()
    }

    private companion object {
        const val DATABASE_NAME = "finance-migration-test"
    }
}

package com.yandex.school.casheye.data.finance.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yandex.school.casheye.data.finance.database.entity.AccountEntity
import com.yandex.school.casheye.data.finance.database.entity.CategoryEntity
import com.yandex.school.casheye.data.finance.database.entity.PendingEntityType
import com.yandex.school.casheye.data.finance.database.entity.PendingOperationType
import com.yandex.school.casheye.data.finance.database.entity.TransactionEntity
import com.yandex.school.casheye.data.finance.database.model.TransactionCommandSnapshot
import com.yandex.school.casheye.domain.finance.SaveAccountCommand
import com.yandex.school.casheye.domain.finance.SaveTransactionCommand
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class OfflineWriteDaoTest {
    private lateinit var database: FinanceDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, FinanceDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun localWritesPersistExactValuesRelationsAndDependency() =
        runTest {
            database.categoryDao().upsertAll(listOf(category))
            val accountWrite = database.offlineWriteDao().createAccount(accountCommand(), NOW)
            val transactionWrite =
                database.offlineWriteDao().createTransaction(
                    transactionCommand(accountId = accountWrite.localId),
                    NOW.plusSeconds(1),
                )

            val account = database.accountDao().getById(accountWrite.localId)
            val transaction = database.transactionDao().getById(transactionWrite.localId)
            val operations = database.pendingOperationDao().getAll()

            assertEquals(-1, accountWrite.localId)
            assertEquals("87.6610", account?.balance)
            assertEquals("12.3400", transaction?.transaction?.amount)
            assertEquals(NOW.plusSeconds(30).toEpochMilli(), transaction?.transaction?.transactionDate)
            assertEquals(accountWrite.localId, transaction?.account?.id)
            assertEquals(category.id, transaction?.category?.id)
            assertEquals(accountWrite.operationId, operations[1].dependsOnOperationId)
        }

    @Test
    fun completingAccountCreateReplacesTemporaryIdAndUnblocksTransaction() =
        runTest {
            database.categoryDao().upsertAll(listOf(category))
            val accountWrite = database.offlineWriteDao().createAccount(accountCommand(), NOW)
            database.offlineWriteDao().createTransaction(
                transactionCommand(accountId = accountWrite.localId),
                NOW.plusSeconds(1),
            )
            val sentAccountOperation = database.pendingOperationDao().getById(accountWrite.operationId)!!

            database.offlineWriteDao().completeAccountCreate(
                sentOperations = listOf(sentAccountOperation),
                serverAccount =
                    AccountEntity(
                        id = 101,
                        name = "Server account",
                        emoji = "💳",
                        balance = "100.0010",
                        currency = "RUB",
                    ),
            )

            val remaining = database.pendingOperationDao().getAll().single()
            val snapshot = Json.decodeFromString<TransactionCommandSnapshot>(remaining.payload)
            val transaction = database.transactionDao().getForPeriod(null, 0, Long.MAX_VALUE).single()

            assertNull(database.accountDao().getById(accountWrite.localId))
            assertEquals("87.6610", database.accountDao().getById(101)?.balance)
            assertEquals(101, transaction.account.id)
            assertEquals(101, transaction.transaction.accountId)
            assertEquals(101, remaining.relatedAccountId)
            assertNull(remaining.dependsOnOperationId)
            assertEquals(101, snapshot.accountId)
        }

    @Test
    fun completionKeepsNewerLocalEditInsteadOfServerVersion() =
        runTest {
            database.accountDao().upsert(
                AccountEntity(5, "Original", "💳", "10.00", "RUB"),
            )
            database.offlineWriteDao().updateAccount(
                accountCommand(id = 5, name = "First local"),
                NOW,
            )
            val sent = database.pendingOperationDao().getAll().single()
            database.offlineWriteDao().updateAccount(
                accountCommand(id = 5, name = "Latest local"),
                NOW.plusSeconds(1),
            )

            database.offlineWriteDao().completeAccountUpdate(
                sentOperations = listOf(sent),
                serverAccount = AccountEntity(5, "Server", "💳", "10.00", "RUB"),
            )

            assertEquals("Latest local", database.accountDao().getById(5)?.name)
            assertEquals(1, database.pendingOperationDao().getAll().size)
        }

    @Test
    fun transactionEditsReverseOldBalanceEffectAndApplyLatestValues() =
        runTest {
            database.accountDao().upsert(AccountEntity(5, "Main", "💳", "100.00", "RUB"))
            database.accountDao().upsert(AccountEntity(6, "Reserve", "💳", "50.00", "RUB"))
            database.categoryDao().upsertAll(listOf(category, incomeCategory))
            val write =
                database.offlineWriteDao().createTransaction(
                    transactionCommand(accountId = 5).copy(amount = BigDecimal("10.00")),
                    NOW,
                )

            assertEquals("90.00", database.accountDao().getById(5)?.balance)

            database.offlineWriteDao().updateTransaction(
                transactionCommand(accountId = 5).copy(
                    id = write.localId,
                    categoryId = category.id,
                    amount = BigDecimal("25.00"),
                ),
                NOW.plusSeconds(1),
            )

            assertEquals("75.00", database.accountDao().getById(5)?.balance)

            database.offlineWriteDao().updateTransaction(
                transactionCommand(accountId = 5).copy(
                    id = write.localId,
                    categoryId = incomeCategory.id,
                    amount = BigDecimal("5.00"),
                ),
                NOW.plusSeconds(2),
            )

            assertEquals("105.00", database.accountDao().getById(5)?.balance)

            database.offlineWriteDao().updateTransaction(
                transactionCommand(accountId = 6).copy(
                    id = write.localId,
                    categoryId = category.id,
                    amount = BigDecimal("20.00"),
                ),
                NOW.plusSeconds(3),
            )

            assertEquals("100.00", database.accountDao().getById(5)?.balance)
            assertEquals("30.00", database.accountDao().getById(6)?.balance)
            assertEquals(1, database.pendingOperationDao().getAll().size)
        }

    @Test
    fun periodCleanupPreservesTransactionWithPendingEdit() =
        runTest {
            database.accountDao().upsert(AccountEntity(5, "Main", "💳", "100.00", "RUB"))
            database.categoryDao().upsertAll(listOf(category))
            database.transactionDao().upsert(
                TransactionEntity(
                    id = 15,
                    accountId = 5,
                    categoryId = category.id,
                    amount = "4.00",
                    transactionDate = NOW.toEpochMilli(),
                    comment = null,
                    createdAt = NOW.toEpochMilli(),
                    updatedAt = NOW.toEpochMilli(),
                ),
            )
            database.offlineWriteDao().updateTransaction(
                transactionCommand(accountId = 5).copy(id = 15),
                NOW.plusSeconds(1),
            )

            database.transactionDao().deleteSyncedForPeriod(
                NOW.minusSeconds(1).toEpochMilli(),
                NOW.plusSeconds(60).toEpochMilli(),
            )

            val transaction = database.transactionDao().getById(15)
            assertEquals("12.3400", transaction?.transaction?.amount)
            assertEquals(1, database.pendingOperationDao().getAll().size)
        }

    @Test
    fun deletingExpenseRestoresBalanceAndCreatesDeleteTombstone() =
        runTest {
            database.accountDao().upsert(AccountEntity(5, "Main", "💳", "75.00", "RUB"))
            database.categoryDao().upsertAll(listOf(category))
            database.transactionDao().upsert(
                TransactionEntity(
                    id = 15,
                    accountId = 5,
                    categoryId = category.id,
                    amount = "25.00",
                    transactionDate = NOW.toEpochMilli(),
                    comment = null,
                    createdAt = NOW.toEpochMilli(),
                    updatedAt = NOW.toEpochMilli(),
                ),
            )

            database.offlineWriteDao().deleteTransaction(15, NOW.plusSeconds(1))

            assertEquals("100.00", database.accountDao().getById(5)?.balance)
            assertEquals(null, database.transactionDao().getById(15))
            val operation = database.pendingOperationDao().getAll().single()
            assertEquals(PendingOperationType.DELETE, operation.operationType)
            assertEquals(PendingEntityType.TRANSACTION, operation.entityType)
            assertEquals(5, operation.relatedAccountId)
        }

    @Test
    fun deletingOfflineAccountCancelsItsCreates() =
        runTest {
            database.categoryDao().upsertAll(listOf(category))
            val account = database.offlineWriteDao().createAccount(accountCommand(), NOW)
            database.offlineWriteDao().createTransaction(
                transactionCommand(account.localId),
                NOW.plusSeconds(1),
            )

            val deletedTransactions =
                database.offlineWriteDao().deleteAccount(account.localId, NOW.plusSeconds(2))

            assertEquals(1, deletedTransactions)
            assertEquals(null, database.accountDao().getById(account.localId))
            assertTrue(database.pendingOperationDao().getAll().isEmpty())
        }

    @Test
    fun createResponseAfterLocalDeleteQueuesServerDelete() =
        runTest {
            database.accountDao().upsert(AccountEntity(5, "Main", "💳", "100.00", "RUB"))
            database.categoryDao().upsertAll(listOf(category))
            val write =
                database.offlineWriteDao().createTransaction(
                    transactionCommand(accountId = 5),
                    NOW,
                )
            val sentOperation = database.pendingOperationDao().getById(write.operationId)!!

            database.offlineWriteDao().deleteTransaction(write.localId, NOW.plusSeconds(1))
            database.offlineWriteDao().completeTransactionCreate(
                sentOperations = listOf(sentOperation),
                serverTransaction =
                    TransactionEntity(
                        id = 50,
                        accountId = 5,
                        categoryId = category.id,
                        amount = "12.3400",
                        transactionDate = NOW.plusSeconds(30).toEpochMilli(),
                        comment = "Offline",
                        createdAt = NOW.toEpochMilli(),
                        updatedAt = NOW.toEpochMilli(),
                    ),
            )

            assertEquals(null, database.transactionDao().getById(50))
            val operation = database.pendingOperationDao().getAll().single()
            assertEquals(PendingOperationType.DELETE, operation.operationType)
            assertEquals(50, operation.localEntityId)
        }

    private fun accountCommand(
        id: Int? = null,
        name: String = "Offline account",
    ) = SaveAccountCommand(
        id = id,
        name = name,
        emoji = "💳",
        balance = BigDecimal("100.0010"),
        currency = "RUB",
    )

    private fun transactionCommand(accountId: Int) =
        SaveTransactionCommand(
            id = null,
            accountId = accountId,
            categoryId = category.id,
            amount = BigDecimal("12.3400"),
            transactionDate = NOW.plusSeconds(30),
            comment = "Offline",
        )

    private companion object {
        val NOW: Instant = Instant.parse("2026-07-22T10:00:00Z")
        val category = CategoryEntity(8, "Food", "🍜", false)
        val incomeCategory = CategoryEntity(9, "Salary", "💰", true)
    }
}

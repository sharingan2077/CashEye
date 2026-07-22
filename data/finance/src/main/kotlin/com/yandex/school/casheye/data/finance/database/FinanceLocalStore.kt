package com.yandex.school.casheye.data.finance.database

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.data.finance.database.entity.PendingEntityType
import com.yandex.school.casheye.data.finance.database.mapper.toDomain
import com.yandex.school.casheye.data.finance.database.mapper.toEntity
import com.yandex.school.casheye.data.finance.dto.AccountDto
import com.yandex.school.casheye.data.finance.dto.AccountResponseDto
import com.yandex.school.casheye.data.finance.dto.CategoryDto
import com.yandex.school.casheye.data.finance.dto.TransactionResponseDto
import com.yandex.school.casheye.data.finance.mapper.toDomain as toNetworkDomain
import com.yandex.school.casheye.domain.finance.SaveAccountCommand
import com.yandex.school.casheye.domain.finance.SaveTransactionCommand
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.time.Instant

interface FinanceLocalStore {
    suspend fun getAccounts(): List<Account>

    suspend fun getAccount(id: Int): Account?

    suspend fun getCategories(isIncome: Boolean): List<Category>

    suspend fun getTransaction(id: Int): Transaction?

    suspend fun getTransactions(
        accountId: Int?,
        startInclusive: Instant,
        endInclusive: Instant,
    ): List<Transaction>

    suspend fun refreshAccounts(accounts: List<AccountDto>)

    suspend fun refreshCategories(categories: List<CategoryDto>)

    suspend fun refreshPeriod(
        accounts: List<AccountDto>,
        categories: List<CategoryDto>,
        transactions: List<TransactionResponseDto>,
        startInclusive: Instant,
        endInclusive: Instant,
    )

    suspend fun cacheAccount(account: AccountResponseDto)

    suspend fun cacheTransaction(transaction: TransactionResponseDto)

    suspend fun saveAccount(command: SaveAccountCommand, now: Instant)

    suspend fun saveTransaction(command: SaveTransactionCommand, now: Instant)
}

@Inject
@SingleIn(AppScope::class)
class RoomFinanceLocalStore(
    context: Context,
) : FinanceLocalStore {
    private val database =
        Room.databaseBuilder(
            context.applicationContext,
            FinanceDatabase::class.java,
            DATABASE_NAME,
        ).build()

    override suspend fun getAccounts(): List<Account> = database.accountDao().getAll().map { it.toDomain() }

    override suspend fun getAccount(id: Int): Account? = database.accountDao().getById(id)?.toDomain()

    override suspend fun getCategories(isIncome: Boolean): List<Category> =
        database.categoryDao().getByType(isIncome).map { it.toDomain() }

    override suspend fun getTransaction(id: Int): Transaction? = database.transactionDao().getById(id)?.toDomain()

    override suspend fun getTransactions(
        accountId: Int?,
        startInclusive: Instant,
        endInclusive: Instant,
    ): List<Transaction> =
        database.transactionDao()
            .getForPeriod(accountId, startInclusive.toEpochMilli(), endInclusive.toEpochMilli())
            .map { it.toDomain() }

    override suspend fun refreshAccounts(accounts: List<AccountDto>) {
        database.withTransaction {
            val pendingIds = database.pendingOperationDao().getPendingEntityIds(PendingEntityType.ACCOUNT).toSet()
            database.accountDao().upsertAll(
                accounts.filterNot { it.id in pendingIds }.map { it.toNetworkDomain().toEntity() },
            )
        }
    }

    override suspend fun refreshCategories(categories: List<CategoryDto>) {
        database.categoryDao().upsertAll(categories.map { it.toNetworkDomain().toEntity() })
    }

    override suspend fun refreshPeriod(
        accounts: List<AccountDto>,
        categories: List<CategoryDto>,
        transactions: List<TransactionResponseDto>,
        startInclusive: Instant,
        endInclusive: Instant,
    ) {
        database.withTransaction {
            val pendingAccounts = database.pendingOperationDao().getPendingEntityIds(PendingEntityType.ACCOUNT).toSet()
            val pendingTransactions =
                database.pendingOperationDao().getPendingEntityIds(PendingEntityType.TRANSACTION).toSet()

            database.accountDao().upsertAll(
                accounts.filterNot { it.id in pendingAccounts }.map { it.toNetworkDomain().toEntity() },
            )
            database.categoryDao().upsertAll(categories.map { it.toNetworkDomain().toEntity() })
            database.transactionDao().deleteSyncedForPeriod(
                startInclusive.toEpochMilli(),
                endInclusive.toEpochMilli(),
            )
            database.transactionDao().upsertAll(
                transactions.filterNot { it.id in pendingTransactions }.map { it.toNetworkDomain().toEntity() },
            )
        }
    }

    override suspend fun cacheAccount(account: AccountResponseDto) {
        database.withTransaction {
            val pendingIds = database.pendingOperationDao().getPendingEntityIds(PendingEntityType.ACCOUNT)
            if (account.id !in pendingIds) database.accountDao().upsert(account.toNetworkDomain().toEntity())
        }
    }

    override suspend fun cacheTransaction(transaction: TransactionResponseDto) {
        database.withTransaction {
            val pendingAccounts = database.pendingOperationDao().getPendingEntityIds(PendingEntityType.ACCOUNT)
            val pendingTransactions = database.pendingOperationDao().getPendingEntityIds(PendingEntityType.TRANSACTION)
            if (transaction.account.id !in pendingAccounts) {
                database.accountDao().upsert(transaction.account.toNetworkDomain().toEntity())
            }
            database.categoryDao().upsertAll(listOf(transaction.category.toNetworkDomain().toEntity()))
            if (transaction.id !in pendingTransactions) {
                database.transactionDao().upsert(transaction.toNetworkDomain().toEntity())
            }
        }
    }

    override suspend fun saveAccount(command: SaveAccountCommand, now: Instant) {
        if (command.id == null) {
            database.offlineWriteDao().createAccount(command, now)
        } else {
            database.offlineWriteDao().updateAccount(command, now)
        }
    }

    override suspend fun saveTransaction(command: SaveTransactionCommand, now: Instant) {
        if (command.id == null) {
            database.offlineWriteDao().createTransaction(command, now)
        } else {
            database.offlineWriteDao().updateTransaction(command, now)
        }
    }

    private companion object {
        const val DATABASE_NAME = "finance.db"
    }
}

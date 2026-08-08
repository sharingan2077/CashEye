package com.yandex.school.casheye.data.finance.database

import androidx.room.withTransaction
import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.data.finance.database.entity.AccountTransactionHistoryVerificationEntity
import com.yandex.school.casheye.data.finance.database.entity.PendingEntityType
import com.yandex.school.casheye.data.finance.database.entity.PendingOperationEntity
import com.yandex.school.casheye.data.finance.database.entity.TransactionEntity
import com.yandex.school.casheye.data.finance.database.mapper.toDomain
import com.yandex.school.casheye.data.finance.database.mapper.toEntity
import com.yandex.school.casheye.data.finance.dto.AccountDto
import com.yandex.school.casheye.data.finance.dto.AccountResponseDto
import com.yandex.school.casheye.data.finance.dto.CategoryDto
import com.yandex.school.casheye.data.finance.dto.TransactionDto
import com.yandex.school.casheye.data.finance.dto.TransactionResponseDto
import com.yandex.school.casheye.domain.finance.editor.SaveAccountCommand
import com.yandex.school.casheye.domain.finance.editor.SaveTransactionCommand
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import com.yandex.school.casheye.data.finance.mapper.toDomain as toNetworkDomain

/** Room-backed [FinanceLocalStore] that applies local-wins merge rules and creates durable outbox entries. */
@Inject
@SingleIn(AppScope::class)
class RoomFinanceLocalStore(
    databaseProvider: FinanceDatabaseProvider,
) : FinanceLocalStore {
    private val database = databaseProvider.database

    override fun observeAccounts(): Flow<List<Account>> =
        database.accountDao().observeAll().map {
            it.map { account -> account.toDomain() }
        }

    override fun observeTransactions(
        accountId: Int?,
        startInclusive: Instant,
        endInclusive: Instant,
    ): Flow<List<Transaction>> =
        database
            .transactionDao()
            .observeForPeriod(
                accountId,
                startInclusive.toEpochMilli(),
                endInclusive.toEpochMilli(),
            ).map {
                it.map { transaction ->
                    transaction.toDomain()
                }
            }

    override suspend fun getAccounts(): List<Account> = database.accountDao().getAll().map { it.toDomain() }

    override suspend fun getAccount(id: Int): Account? = database.accountDao().getById(id)?.toDomain()

    override suspend fun getCategories(isIncome: Boolean): List<Category> =
        database.categoryDao().getByType(isIncome).map {
            it.toDomain()
        }

    override suspend fun hasUsableCache(): Boolean = database.categoryDao().count() > 0

    override suspend fun getTransaction(id: Int): Transaction? = database.transactionDao().getById(id)?.toDomain()

    override suspend fun getTransactions(
        accountId: Int?,
        startInclusive: Instant,
        endInclusive: Instant,
    ): List<Transaction> =
        database
            .transactionDao()
            .getForPeriod(
                accountId,
                startInclusive.toEpochMilli(),
                endInclusive.toEpochMilli(),
            ).map {
                it.toDomain()
            }

    override suspend fun refreshAccounts(accounts: List<AccountDto>) =
        database.withTransaction {
            val pendingIds = pendingAccountIds()
            database.accountDao().upsertAll(
                accounts.filterNot { it.id in pendingIds }.map { it.toNetworkDomain().toEntity() },
            )
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
    ) = database.withTransaction {
        val pendingAccounts = pendingAccountIds()
        val pendingTransactions =
            database
                .pendingOperationDao()
                .getPendingEntityIds(
                    PendingEntityType.TRANSACTION,
                ).toSet()
        database.accountDao().upsertAll(
            accounts.filterNot { it.id in pendingAccounts }.map { it.toNetworkDomain().toEntity() },
        )
        database.categoryDao().upsertAll(categories.map { it.toNetworkDomain().toEntity() })
        database.transactionDao().upsertAll(
            transactions
                .filterNot { it.id in pendingTransactions }
                .map { transaction ->
                    transaction
                        .toNetworkDomain()
                        .toEntity()
                        .copy(
                            currency =
                                database.transactionDao().getCurrencyById(transaction.id)
                                    ?: transaction.account.currency,
                        )
                },
        )
    }

    override suspend fun cacheAccount(account: AccountResponseDto) =
        database.withTransaction {
            if (account.id !in pendingAccountIds()) database.accountDao().upsert(account.toNetworkDomain().toEntity())
        }

    override suspend fun cacheTransaction(transaction: TransactionResponseDto) =
        database.withTransaction {
            val pendingAccounts = pendingAccountIds()
            val pendingTransactions = database.pendingOperationDao().getPendingEntityIds(PendingEntityType.TRANSACTION)
            if (transaction.account.id !in
                pendingAccounts
            ) {
                database.accountDao().upsert(transaction.account.toNetworkDomain().toEntity())
            }
            database.categoryDao().upsertAll(listOf(transaction.category.toNetworkDomain().toEntity()))
            if (transaction.id !in
                pendingTransactions
            ) {
                database.transactionDao().upsert(toStoredTransaction(transaction))
            }
        }

    private suspend fun toStoredTransaction(transaction: TransactionResponseDto): TransactionEntity =
        transaction.toNetworkDomain().toEntity().copy(
            currency =
                database.transactionDao().getCurrencyById(transaction.id) ?: transaction.account.currency,
        )

    override suspend fun cacheCompleteAccountTransactionHistory(
        accountId: Int,
        transactions: List<TransactionResponseDto>,
        verifiedAt: Instant,
    ) = database.withTransaction {
        for (transaction in transactions) {
            cacheTransaction(transaction)
        }
        if (database.accountDao().getById(accountId) !=
            null
        ) {
            database.accountTransactionHistoryVerificationDao().upsert(
                AccountTransactionHistoryVerificationEntity(accountId, verifiedAt.toEpochMilli()),
            )
        }
    }

    private suspend fun pendingAccountIds(): Set<Int> =
        database.pendingOperationDao().run {
            getPendingEntityIds(PendingEntityType.ACCOUNT).toSet() + getPendingRelatedAccountIds().filterNotNull()
        }

    override suspend fun saveAccount(
        command: SaveAccountCommand,
        now: Instant,
    ) {
        if (command.id ==
            null
        ) {
            database.offlineWriteDao().createAccount(command, now)
        } else {
            database.offlineWriteDao().updateAccount(command, now)
        }
    }

    override suspend fun saveTransaction(
        command: SaveTransactionCommand,
        now: Instant,
    ) {
        if (command.id ==
            null
        ) {
            database.offlineWriteDao().createTransaction(command, now)
        } else {
            database.offlineWriteDao().updateTransaction(command, now)
        }
    }

    override suspend fun getAccountTransactionCount(id: Int): Int = database.transactionDao().countByAccountId(id)

    override suspend fun isAccountTransactionHistoryVerified(id: Int): Boolean =
        database.accountTransactionHistoryVerificationDao().isVerified(id)

    override suspend fun deleteTransaction(
        id: Int,
        now: Instant,
    ) {
        database.offlineWriteDao().deleteTransaction(id, now)
    }

    override suspend fun deleteAccount(
        id: Int,
        now: Instant,
    ): Int = database.offlineWriteDao().deleteAccount(id, now)

    internal suspend fun getPendingOperations(): List<PendingOperationEntity> = database.pendingOperationDao().getAll()

    internal suspend fun completeAccountCreate(
        sentOperations: List<PendingOperationEntity>,
        response: AccountDto,
    ) {
        database.offlineWriteDao().completeAccountCreate(sentOperations, response.toNetworkDomain().toEntity())
    }

    internal suspend fun completeAccountUpdate(
        sentOperations: List<PendingOperationEntity>,
        response: AccountDto,
    ) {
        database.offlineWriteDao().completeAccountUpdate(sentOperations, response.toNetworkDomain().toEntity())
    }

    internal suspend fun completeTransactionCreate(
        sentOperations: List<PendingOperationEntity>,
        response: TransactionDto,
    ) {
        database.offlineWriteDao().completeTransactionCreate(
            sentOperations,
            TransactionEntity(
                response.id,
                response.accountId,
                response.categoryId,
                response.amount,
                database.accountDao().getById(response.accountId)?.currency ?: CurrencyCode.RUB.isoCode,
                response.transactionDate.toEpochMilli(),
                response.comment,
                response.createdAt.toEpochMilli(),
                response.updatedAt.toEpochMilli(),
            ),
        )
    }

    internal suspend fun completeTransactionUpdate(
        sentOperations: List<PendingOperationEntity>,
        response: TransactionResponseDto,
    ) {
        database.offlineWriteDao().completeTransactionUpdate(sentOperations, response.toNetworkDomain().toEntity())
    }

    internal suspend fun completeDelete(sentOperations: List<PendingOperationEntity>) {
        database.offlineWriteDao().completeDelete(sentOperations)
    }
}

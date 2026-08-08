package com.yandex.school.casheye.data.finance.database

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.data.finance.dto.AccountDto
import com.yandex.school.casheye.data.finance.dto.AccountResponseDto
import com.yandex.school.casheye.data.finance.dto.CategoryDto
import com.yandex.school.casheye.data.finance.dto.TransactionResponseDto
import com.yandex.school.casheye.domain.finance.editor.SaveAccountCommand
import com.yandex.school.casheye.domain.finance.editor.SaveTransactionCommand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant

/**
 * Local source of truth for finance data.
 *
 * Implementations preserve pending local operations while incorporating remote snapshots; they do
 * not perform network work themselves.
 */
interface FinanceLocalStore {
    fun observeAccounts(): Flow<List<Account>> = flow { emit(getAccounts()) }

    fun observeTransactions(
        accountId: Int?,
        startInclusive: Instant,
        endInclusive: Instant,
    ): Flow<List<Transaction>> =
        flow {
            emit(getTransactions(accountId, startInclusive, endInclusive))
        }

    suspend fun getAccounts(): List<Account>

    suspend fun getAccount(id: Int): Account?

    suspend fun getCategories(isIncome: Boolean): List<Category>

    suspend fun hasUsableCache(): Boolean

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

    suspend fun cacheCompleteAccountTransactionHistory(
        accountId: Int,
        transactions: List<TransactionResponseDto>,
        verifiedAt: Instant,
    )

    suspend fun isAccountTransactionHistoryVerified(id: Int): Boolean

    suspend fun saveAccount(
        command: SaveAccountCommand,
        now: Instant,
    )

    suspend fun saveTransaction(
        command: SaveTransactionCommand,
        now: Instant,
    )

    suspend fun getAccountTransactionCount(id: Int): Int

    suspend fun deleteTransaction(
        id: Int,
        now: Instant,
    )

    suspend fun deleteAccount(
        id: Int,
        now: Instant,
    ): Int
}

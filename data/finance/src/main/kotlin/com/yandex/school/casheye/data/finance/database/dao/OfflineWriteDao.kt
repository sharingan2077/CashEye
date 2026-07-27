package com.yandex.school.casheye.data.finance.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.yandex.school.casheye.data.finance.database.entity.AccountEntity
import com.yandex.school.casheye.data.finance.database.entity.CategoryEntity
import com.yandex.school.casheye.data.finance.database.entity.PendingEntityType
import com.yandex.school.casheye.data.finance.database.entity.PendingOperationEntity
import com.yandex.school.casheye.data.finance.database.entity.PendingOperationType
import com.yandex.school.casheye.data.finance.database.entity.TransactionEntity
import com.yandex.school.casheye.data.finance.database.model.AccountCommandSnapshot
import com.yandex.school.casheye.data.finance.database.model.LocalWriteResult
import com.yandex.school.casheye.data.finance.database.model.TransactionCommandSnapshot
import com.yandex.school.casheye.domain.finance.editor.SaveAccountCommand
import com.yandex.school.casheye.domain.finance.editor.SaveTransactionCommand
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.Instant

@Dao
internal abstract class OfflineWriteDao {
    @Transaction
    open suspend fun createAccount(
        command: SaveAccountCommand,
        now: Instant,
    ): LocalWriteResult {
        require(command.id == null) { "Offline account creation requires a command without an id" }
        val localId = nextAccountId()
        insertAccount(
            AccountEntity(
                id = localId,
                name = command.name,
                emoji = command.emoji,
                balance = command.balance.toPlainString(),
                currency = command.currency.isoCode,
            ),
        )
        val snapshot =
            AccountCommandSnapshot(
                id = localId,
                name = command.name,
                emoji = command.emoji,
                balance = command.balance.toPlainString(),
                currency = command.currency.isoCode,
            )
        val operationId =
            insertOperation(
                PendingOperationEntity(
                    entityType = PendingEntityType.ACCOUNT,
                    operationType = PendingOperationType.CREATE,
                    localEntityId = localId,
                    relatedAccountId = localId,
                    dependsOnOperationId = null,
                    createdAt = now.toEpochMilli(),
                    payload = json.encodeToString(snapshot),
                ),
            )
        return LocalWriteResult(localId = localId, operationId = operationId)
    }

    @Transaction
    open suspend fun createTransaction(
        command: SaveTransactionCommand,
        now: Instant,
    ): LocalWriteResult {
        require(command.id == null) { "Offline transaction creation requires a command without an id" }
        val localId = nextTransactionId()
        val account = checkNotNull(accountById(command.accountId)) { "Account ${command.accountId} was not found" }
        insertTransaction(
            TransactionEntity(
                id = localId,
                accountId = command.accountId,
                categoryId = command.categoryId,
                amount = command.amount.toPlainString(),
                currency = account.currency,
                transactionDate = command.transactionDate.toEpochMilli(),
                comment = command.comment,
                createdAt = now.toEpochMilli(),
                updatedAt = now.toEpochMilli(),
            ),
        )
        adjustAccountBalance(
            accountId = command.accountId,
            categoryId = command.categoryId,
            amount = command.amount,
        )
        val snapshot =
            TransactionCommandSnapshot(
                id = localId,
                accountId = command.accountId,
                categoryId = command.categoryId,
                amount = command.amount.toPlainString(),
                transactionDate = command.transactionDate.toEpochMilli(),
                comment = command.comment,
            )
        val operationId =
            insertOperation(
                PendingOperationEntity(
                    entityType = PendingEntityType.TRANSACTION,
                    operationType = PendingOperationType.CREATE,
                    localEntityId = localId,
                    relatedAccountId = command.accountId,
                    dependsOnOperationId = findAccountCreateOperation(command.accountId),
                    createdAt = now.toEpochMilli(),
                    payload = json.encodeToString(snapshot),
                ),
            )
        return LocalWriteResult(localId = localId, operationId = operationId)
    }

    @Transaction
    open suspend fun updateAccount(
        command: SaveAccountCommand,
        now: Instant,
    ): LocalWriteResult {
        val localId = requireNotNull(command.id) { "Offline account update requires an id" }
        check(accountById(localId) != null) { "Account $localId was not found" }
        updateAccountRow(
            AccountEntity(
                id = localId,
                name = command.name,
                emoji = command.emoji,
                balance = command.balance.toPlainString(),
                currency = command.currency.isoCode,
            ),
        )
        val payload =
            json.encodeToString(
                AccountCommandSnapshot(
                    id = localId,
                    name = command.name,
                    emoji = command.emoji,
                    balance = command.balance.toPlainString(),
                    currency = command.currency.isoCode,
                ),
            )
        val createOperation = findCreateOperation(PendingEntityType.ACCOUNT, localId)
        val operationId =
            if (createOperation != null) {
                updateOperation(createOperation.copy(payload = payload, relatedAccountId = localId))
                createOperation.id
            } else {
                insertOperation(
                    PendingOperationEntity(
                        entityType = PendingEntityType.ACCOUNT,
                        operationType = PendingOperationType.UPDATE,
                        localEntityId = localId,
                        relatedAccountId = localId,
                        dependsOnOperationId = null,
                        createdAt = now.toEpochMilli(),
                        payload = payload,
                    ),
                )
            }
        return LocalWriteResult(localId = localId, operationId = operationId)
    }

    @Transaction
    open suspend fun updateTransaction(
        command: SaveTransactionCommand,
        now: Instant,
    ): LocalWriteResult {
        val localId = requireNotNull(command.id) { "Offline transaction update requires an id" }
        val existing = checkNotNull(transactionById(localId)) { "Transaction $localId was not found" }
        val targetAccount =
            checkNotNull(accountById(command.accountId)) { "Account ${command.accountId} was not found" }
        adjustAccountBalance(
            accountId = existing.accountId,
            categoryId = existing.categoryId,
            amount = BigDecimal(existing.amount),
            reverse = true,
        )
        updateTransactionRow(
            existing.copy(
                accountId = command.accountId,
                categoryId = command.categoryId,
                amount = command.amount.toPlainString(),
                currency = if (existing.accountId == command.accountId) existing.currency else targetAccount.currency,
                transactionDate = command.transactionDate.toEpochMilli(),
                comment = command.comment,
                updatedAt = now.toEpochMilli(),
            ),
        )
        adjustAccountBalance(
            accountId = command.accountId,
            categoryId = command.categoryId,
            amount = command.amount,
        )
        val payload =
            json.encodeToString(
                TransactionCommandSnapshot(
                    id = localId,
                    accountId = command.accountId,
                    categoryId = command.categoryId,
                    amount = command.amount.toPlainString(),
                    transactionDate = command.transactionDate.toEpochMilli(),
                    comment = command.comment,
                ),
            )
        val accountDependency = findAccountCreateOperation(command.accountId)
        val createOperation = findCreateOperation(PendingEntityType.TRANSACTION, localId)
        val operationId =
            if (createOperation != null) {
                updateOperation(
                    createOperation.copy(
                        relatedAccountId = command.accountId,
                        dependsOnOperationId = accountDependency,
                        payload = payload,
                    ),
                )
                createOperation.id
            } else {
                insertOperation(
                    PendingOperationEntity(
                        entityType = PendingEntityType.TRANSACTION,
                        operationType = PendingOperationType.UPDATE,
                        localEntityId = localId,
                        relatedAccountId = command.accountId,
                        dependsOnOperationId = accountDependency,
                        createdAt = now.toEpochMilli(),
                        payload = payload,
                    ),
                )
            }
        return LocalWriteResult(localId = localId, operationId = operationId)
    }

    @Transaction
    open suspend fun deleteTransaction(
        id: Int,
        now: Instant,
    ) {
        val existing = checkNotNull(transactionById(id)) { "Transaction $id was not found" }
        deleteTransactionInternal(existing, now)
    }

    @Transaction
    open suspend fun deleteAccount(
        id: Int,
        now: Instant,
    ): Int {
        check(accountById(id) != null) { "Account $id was not found" }
        val transactions = transactionsByAccount(id)
        transactions.forEach { deleteTransactionInternal(it, now) }
        deleteOperationsForEntity(PendingEntityType.ACCOUNT, id)
        check(deleteAccountRow(id) == 1) { "Account $id was not found" }
        if (id > 0) {
            insertOperation(
                PendingOperationEntity(
                    entityType = PendingEntityType.ACCOUNT,
                    operationType = PendingOperationType.DELETE,
                    localEntityId = id,
                    relatedAccountId = id,
                    dependsOnOperationId = null,
                    createdAt = now.toEpochMilli(),
                    payload = "",
                ),
            )
        }
        return transactions.size
    }

    @Transaction
    open suspend fun completeAccountCreate(
        sentOperations: List<PendingOperationEntity>,
        serverAccount: AccountEntity,
    ) {
        val temporaryId = sentOperations.first().localEntityId
        val serverId = serverAccount.id
        require(temporaryId < 0) { "Temporary ids must be negative" }
        require(serverId > 0) { "Server ids must be positive" }
        val unchangedOperationIds = unchangedOperationIds(sentOperations)

        if (accountById(temporaryId) == null) {
            insertOperation(
                PendingOperationEntity(
                    entityType = PendingEntityType.ACCOUNT,
                    operationType = PendingOperationType.DELETE,
                    localEntityId = serverId,
                    relatedAccountId = serverId,
                    dependsOnOperationId = null,
                    createdAt = sentOperations.maxOf(PendingOperationEntity::createdAt) + 1,
                    payload = "",
                ),
            )
            unchangedOperationIds.forEach { deleteOperation(it) }
            return
        }

        operationsReferencingAccount(temporaryId).forEach { operation ->
            val updatedPayload =
                when (operation.entityType) {
                    PendingEntityType.ACCOUNT -> {
                        val snapshot = json.decodeFromString<AccountCommandSnapshot>(operation.payload)
                        json.encodeToString(snapshot.copy(id = serverId))
                    }

                    PendingEntityType.TRANSACTION -> {
                        val snapshot = json.decodeFromString<TransactionCommandSnapshot>(operation.payload)
                        json.encodeToString(snapshot.copy(accountId = serverId))
                    }
                }
            updateOperation(
                operation.copy(
                    operationType =
                        if (operation.entityType == PendingEntityType.ACCOUNT) {
                            PendingOperationType.UPDATE
                        } else {
                            operation.operationType
                        },
                    localEntityId =
                        if (operation.entityType == PendingEntityType.ACCOUNT) {
                            serverId
                        } else {
                            operation.localEntityId
                        },
                    relatedAccountId = serverId,
                    payload = updatedPayload,
                ),
            )
        }

        check(replaceAccountId(temporaryId = temporaryId, serverId = serverId) == 1) {
            "Temporary account $temporaryId was not found"
        }
        unchangedOperationIds.forEach { deleteOperation(it) }
    }

    @Transaction
    open suspend fun completeTransactionCreate(
        sentOperations: List<PendingOperationEntity>,
        serverTransaction: TransactionEntity,
    ) {
        val temporaryId = sentOperations.first().localEntityId
        val serverId = serverTransaction.id
        require(temporaryId < 0) { "Temporary ids must be negative" }
        require(serverId > 0) { "Server ids must be positive" }
        val unchangedOperationIds = unchangedOperationIds(sentOperations)
        val localCurrency = transactionById(temporaryId)?.currency

        if (transactionById(temporaryId) == null) {
            insertOperation(
                PendingOperationEntity(
                    entityType = PendingEntityType.TRANSACTION,
                    operationType = PendingOperationType.DELETE,
                    localEntityId = serverId,
                    relatedAccountId = serverTransaction.accountId,
                    dependsOnOperationId = null,
                    createdAt = sentOperations.maxOf(PendingOperationEntity::createdAt) + 1,
                    payload = "",
                ),
            )
            unchangedOperationIds.forEach { deleteOperation(it) }
            return
        }

        operationsForEntity(PendingEntityType.TRANSACTION, temporaryId).forEach { operation ->
            val snapshot = json.decodeFromString<TransactionCommandSnapshot>(operation.payload)
            updateOperation(
                operation.copy(
                    operationType = PendingOperationType.UPDATE,
                    localEntityId = serverId,
                    payload = json.encodeToString(snapshot.copy(id = serverId)),
                ),
            )
        }

        check(replaceTransactionId(temporaryId = temporaryId, serverId = serverId) == 1) {
            "Temporary transaction $temporaryId was not found"
        }
        unchangedOperationIds.forEach { deleteOperation(it) }
        if (countOperations(PendingEntityType.TRANSACTION, serverId) == 0) {
            upsertTransaction(serverTransaction.copy(currency = localCurrency ?: serverTransaction.currency))
        }
    }

    @Transaction
    open suspend fun completeAccountUpdate(
        sentOperations: List<PendingOperationEntity>,
        serverAccount: AccountEntity,
    ) {
        unchangedOperationIds(sentOperations).forEach { deleteOperation(it) }
        // A local delete may win while this update is in flight. In that case the account row is
        // intentionally absent and its newer DELETE tombstone must remain queued.
        if (accountById(serverAccount.id) == null) return
    }

    @Transaction
    open suspend fun completeTransactionUpdate(
        sentOperations: List<PendingOperationEntity>,
        serverTransaction: TransactionEntity,
    ) {
        val localCurrency = transactionById(serverTransaction.id)?.currency
        unchangedOperationIds(sentOperations).forEach { deleteOperation(it) }
        if (countOperations(PendingEntityType.TRANSACTION, serverTransaction.id) == 0) {
            upsertTransaction(serverTransaction.copy(currency = localCurrency ?: serverTransaction.currency))
        }
    }

    @Transaction
    open suspend fun completeDelete(sentOperations: List<PendingOperationEntity>) {
        unchangedOperationIds(sentOperations).forEach { deleteOperation(it) }
    }

    private suspend fun unchangedOperationIds(sentOperations: List<PendingOperationEntity>): List<Long> =
        sentOperations.mapNotNull { sent -> sent.id.takeIf { operationById(sent.id) == sent } }

    private suspend fun adjustAccountBalance(
        accountId: Int,
        categoryId: Int,
        amount: BigDecimal,
        reverse: Boolean = false,
    ) {
        val account = checkNotNull(accountById(accountId)) { "Account $accountId was not found" }
        val category = checkNotNull(categoryById(categoryId)) { "Category $categoryId was not found" }
        val transactionDelta = if (category.isIncome) amount else amount.negate()
        val balanceDelta = if (reverse) transactionDelta.negate() else transactionDelta
        updateAccountRow(
            account.copy(
                balance = BigDecimal(account.balance).add(balanceDelta).toPlainString(),
            ),
        )
    }

    private suspend fun deleteTransactionInternal(
        transaction: TransactionEntity,
        now: Instant,
    ) {
        adjustAccountBalance(
            accountId = transaction.accountId,
            categoryId = transaction.categoryId,
            amount = BigDecimal(transaction.amount),
            reverse = true,
        )
        deleteOperationsForEntity(PendingEntityType.TRANSACTION, transaction.id)
        check(deleteTransactionRow(transaction.id) == 1) { "Transaction ${transaction.id} was not found" }
        if (transaction.id > 0) {
            insertOperation(
                PendingOperationEntity(
                    entityType = PendingEntityType.TRANSACTION,
                    operationType = PendingOperationType.DELETE,
                    localEntityId = transaction.id,
                    relatedAccountId = transaction.accountId,
                    dependsOnOperationId = null,
                    createdAt = now.toEpochMilli(),
                    payload = "",
                ),
            )
        }
    }

    @Query("SELECT COALESCE(MIN(id), 0) - 1 FROM accounts WHERE id < 0")
    protected abstract suspend fun nextAccountId(): Int

    @Query("SELECT COALESCE(MIN(id), 0) - 1 FROM transactions WHERE id < 0")
    protected abstract suspend fun nextTransactionId(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertAccount(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertOperation(operation: PendingOperationEntity): Long

    @Query("SELECT * FROM accounts WHERE id = :id")
    protected abstract suspend fun accountById(id: Int): AccountEntity?

    @Query("SELECT * FROM transactions WHERE id = :id")
    protected abstract suspend fun transactionById(id: Int): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE account_id = :accountId")
    protected abstract suspend fun transactionsByAccount(accountId: Int): List<TransactionEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    protected abstract suspend fun categoryById(id: Int): CategoryEntity?

    @Update
    protected abstract suspend fun updateAccountRow(account: AccountEntity)

    @Update
    protected abstract suspend fun updateTransactionRow(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    protected abstract suspend fun deleteTransactionRow(id: Int): Int

    @Query("DELETE FROM accounts WHERE id = :id")
    protected abstract suspend fun deleteAccountRow(id: Int): Int

    @Update
    protected abstract suspend fun updateOperation(operation: PendingOperationEntity)

    @Upsert
    protected abstract suspend fun upsertTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM pending_operations WHERE id = :operationId")
    protected abstract suspend fun operationById(operationId: Long): PendingOperationEntity?

    @Query(
        "SELECT COUNT(*) FROM pending_operations WHERE entity_type = :entityType AND local_entity_id = :localId",
    )
    protected abstract suspend fun countOperations(
        entityType: PendingEntityType,
        localId: Int,
    ): Int

    @Query(
        """
        SELECT id FROM pending_operations
        WHERE entity_type = 'ACCOUNT'
          AND operation_type = 'CREATE'
          AND local_entity_id = :accountId
        LIMIT 1
        """,
    )
    protected abstract suspend fun findAccountCreateOperation(accountId: Int): Long?

    @Query(
        """
        SELECT * FROM pending_operations
        WHERE entity_type = :entityType
          AND operation_type = 'CREATE'
          AND local_entity_id = :localId
        LIMIT 1
        """,
    )
    protected abstract suspend fun findCreateOperation(
        entityType: PendingEntityType,
        localId: Int,
    ): PendingOperationEntity?

    @Query(
        """
        SELECT * FROM pending_operations
        WHERE (entity_type = 'ACCOUNT' AND local_entity_id = :accountId)
           OR related_account_id = :accountId
        """,
    )
    protected abstract suspend fun operationsReferencingAccount(accountId: Int): List<PendingOperationEntity>

    @Query(
        """
        SELECT * FROM pending_operations
        WHERE entity_type = :entityType AND local_entity_id = :localId
        """,
    )
    protected abstract suspend fun operationsForEntity(
        entityType: PendingEntityType,
        localId: Int,
    ): List<PendingOperationEntity>

    @Query(
        "DELETE FROM pending_operations WHERE entity_type = :entityType AND local_entity_id = :localId",
    )
    protected abstract suspend fun deleteOperationsForEntity(
        entityType: PendingEntityType,
        localId: Int,
    ): Int

    @Query("UPDATE accounts SET id = :serverId WHERE id = :temporaryId")
    protected abstract suspend fun replaceAccountId(
        temporaryId: Int,
        serverId: Int,
    ): Int

    @Query("UPDATE transactions SET id = :serverId WHERE id = :temporaryId")
    protected abstract suspend fun replaceTransactionId(
        temporaryId: Int,
        serverId: Int,
    ): Int

    @Query("DELETE FROM pending_operations WHERE id = :operationId")
    protected abstract suspend fun deleteOperation(operationId: Long): Int

    private companion object {
        val json = Json
    }
}

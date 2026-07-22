package com.yandex.school.casheye.data.finance.database.model

import kotlinx.serialization.Serializable

@Serializable
internal data class AccountCommandSnapshot(
    val id: Int,
    val name: String,
    val emoji: String,
    val balance: String,
    val currency: String,
)

@Serializable
internal data class TransactionCommandSnapshot(
    val id: Int,
    val accountId: Int,
    val categoryId: Int,
    val amount: String,
    val transactionDate: Long,
    val comment: String?,
)

internal data class LocalWriteResult(
    val localId: Int,
    val operationId: Long,
)

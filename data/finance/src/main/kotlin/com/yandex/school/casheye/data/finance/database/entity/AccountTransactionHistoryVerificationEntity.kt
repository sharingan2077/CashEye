package com.yandex.school.casheye.data.finance.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "account_transaction_history_verifications",
    primaryKeys = ["account_id"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
internal data class AccountTransactionHistoryVerificationEntity(
    @ColumnInfo(name = "account_id")
    val accountId: Int,
    @ColumnInfo(name = "verified_at")
    val verifiedAt: Long,
)

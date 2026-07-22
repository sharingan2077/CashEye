package com.yandex.school.casheye.data.finance.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
internal data class AccountEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val emoji: String,
    val balance: String,
    val currency: String,
)

@Entity(tableName = "categories")
internal data class CategoryEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val emoji: String,
    @ColumnInfo(name = "is_income")
    val isIncome: Boolean,
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("account_id"), Index("category_id"), Index("transaction_date")],
)
internal data class TransactionEntity(
    @PrimaryKey
    val id: Int,
    @ColumnInfo(name = "account_id")
    val accountId: Int,
    @ColumnInfo(name = "category_id")
    val categoryId: Int,
    val amount: String,
    @ColumnInfo(name = "transaction_date")
    val transactionDate: Long,
    val comment: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

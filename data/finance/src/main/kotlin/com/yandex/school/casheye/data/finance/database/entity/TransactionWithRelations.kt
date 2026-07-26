package com.yandex.school.casheye.data.finance.database.entity

import androidx.room.Embedded
import androidx.room.Relation

internal data class TransactionWithRelations(
    @Embedded
    val transaction: TransactionEntity,
    @Relation(parentColumn = "account_id", entityColumn = "id")
    val account: AccountEntity,
    @Relation(parentColumn = "category_id", entityColumn = "id")
    val category: CategoryEntity,
)

package com.yandex.school.casheye.data.finance.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

internal enum class PendingEntityType {
    ACCOUNT,
    TRANSACTION,
}

internal enum class PendingOperationType {
    CREATE,
    UPDATE,
    DELETE,
}

@Entity(
    tableName = "pending_operations",
    foreignKeys = [
        ForeignKey(
            entity = PendingOperationEntity::class,
            parentColumns = ["id"],
            childColumns = ["depends_on_operation_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("depends_on_operation_id"),
        Index(value = ["entity_type", "local_entity_id"]),
        Index("related_account_id"),
        Index("created_at"),
    ],
)
internal data class PendingOperationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "entity_type")
    val entityType: PendingEntityType,
    @ColumnInfo(name = "operation_type")
    val operationType: PendingOperationType,
    @ColumnInfo(name = "local_entity_id")
    val localEntityId: Int,
    @ColumnInfo(name = "related_account_id")
    val relatedAccountId: Int?,
    @ColumnInfo(name = "depends_on_operation_id")
    val dependsOnOperationId: Long?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    val payload: String,
)

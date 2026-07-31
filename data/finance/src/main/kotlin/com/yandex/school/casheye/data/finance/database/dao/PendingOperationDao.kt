package com.yandex.school.casheye.data.finance.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.yandex.school.casheye.data.finance.database.entity.PendingEntityType
import com.yandex.school.casheye.data.finance.database.entity.PendingOperationEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface PendingOperationDao {
    @Query("SELECT local_entity_id FROM pending_operations WHERE entity_type = :entityType")
    suspend fun getPendingEntityIds(entityType: PendingEntityType): List<Int>

    @Query("SELECT DISTINCT related_account_id FROM pending_operations WHERE related_account_id IS NOT NULL")
    suspend fun getPendingRelatedAccountIds(): List<Int?>

    @Query("SELECT * FROM pending_operations ORDER BY created_at, id")
    fun observeAll(): Flow<List<PendingOperationEntity>>

    @Query("SELECT * FROM pending_operations ORDER BY created_at, id")
    suspend fun getAll(): List<PendingOperationEntity>

    @Query(
        """
        SELECT * FROM pending_operations
        WHERE depends_on_operation_id IS NULL
        ORDER BY created_at, id
        """,
    )
    suspend fun getReady(): List<PendingOperationEntity>

    @Query("SELECT * FROM pending_operations WHERE id = :id")
    suspend fun getById(id: Long): PendingOperationEntity?

    @Upsert
    suspend fun upsert(operation: PendingOperationEntity): Long

    @Update
    suspend fun update(operation: PendingOperationEntity)

    @Delete
    suspend fun delete(operation: PendingOperationEntity)
}

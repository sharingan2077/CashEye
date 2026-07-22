package com.yandex.school.casheye.data.finance.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yandex.school.casheye.data.finance.database.dao.AccountDao
import com.yandex.school.casheye.data.finance.database.dao.CategoryDao
import com.yandex.school.casheye.data.finance.database.dao.OfflineWriteDao
import com.yandex.school.casheye.data.finance.database.dao.PendingOperationDao
import com.yandex.school.casheye.data.finance.database.dao.TransactionDao
import com.yandex.school.casheye.data.finance.database.entity.AccountEntity
import com.yandex.school.casheye.data.finance.database.entity.CategoryEntity
import com.yandex.school.casheye.data.finance.database.entity.PendingOperationEntity
import com.yandex.school.casheye.data.finance.database.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        PendingOperationEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(FinanceConverters::class)
internal abstract class FinanceDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao

    abstract fun categoryDao(): CategoryDao

    abstract fun transactionDao(): TransactionDao

    abstract fun pendingOperationDao(): PendingOperationDao

    abstract fun offlineWriteDao(): OfflineWriteDao
}

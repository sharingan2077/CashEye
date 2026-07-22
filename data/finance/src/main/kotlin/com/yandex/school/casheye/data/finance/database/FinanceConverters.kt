package com.yandex.school.casheye.data.finance.database

import androidx.room.TypeConverter
import com.yandex.school.casheye.data.finance.database.entity.PendingEntityType
import com.yandex.school.casheye.data.finance.database.entity.PendingOperationType

internal class FinanceConverters {
    @TypeConverter
    fun entityTypeToString(value: PendingEntityType): String = value.name

    @TypeConverter
    fun stringToEntityType(value: String): PendingEntityType = PendingEntityType.valueOf(value)

    @TypeConverter
    fun operationTypeToString(value: PendingOperationType): String = value.name

    @TypeConverter
    fun stringToOperationType(value: String): PendingOperationType = PendingOperationType.valueOf(value)
}

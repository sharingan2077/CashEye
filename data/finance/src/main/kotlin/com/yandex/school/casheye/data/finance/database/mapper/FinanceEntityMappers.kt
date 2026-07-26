package com.yandex.school.casheye.data.finance.database.mapper

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.data.finance.database.entity.AccountEntity
import com.yandex.school.casheye.data.finance.database.entity.CategoryEntity
import com.yandex.school.casheye.data.finance.database.entity.TransactionEntity
import com.yandex.school.casheye.data.finance.database.entity.TransactionWithRelations
import java.math.BigDecimal
import java.time.Instant

internal fun AccountEntity.toDomain(): Account =
    Account(
        id = id,
        name = name,
        emoji = emoji,
        balance = BigDecimal(balance),
        currency = CurrencyCode.fromIsoCode(currency),
    )

internal fun Account.toEntity(): AccountEntity =
    AccountEntity(
        id = id,
        name = name,
        emoji = emoji,
        balance = balance.toPlainString(),
        currency = currency.isoCode,
    )

internal fun CategoryEntity.toDomain(): Category =
    Category(
        id = id,
        name = name,
        emoji = emoji,
        isIncome = isIncome,
    )

internal fun Category.toEntity(): CategoryEntity =
    CategoryEntity(
        id = id,
        name = name,
        emoji = emoji,
        isIncome = isIncome,
    )

internal fun TransactionWithRelations.toDomain(): Transaction =
    Transaction(
        id = transaction.id,
        account = account.toDomain(),
        category = category.toDomain(),
        amount = BigDecimal(transaction.amount),
        currency = CurrencyCode.fromIsoCode(transaction.currency),
        transactionDate = Instant.ofEpochMilli(transaction.transactionDate),
        comment = transaction.comment,
        createdAt = Instant.ofEpochMilli(transaction.createdAt),
        updatedAt = Instant.ofEpochMilli(transaction.updatedAt),
    )

internal fun Transaction.toEntity(): TransactionEntity =
    TransactionEntity(
        id = id,
        accountId = account.id,
        categoryId = category.id,
        amount = amount.toPlainString(),
        currency = currency.isoCode,
        transactionDate = transactionDate.toEpochMilli(),
        comment = comment,
        createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
    )

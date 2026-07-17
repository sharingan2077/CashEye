package com.yandex.school.casheye.data.expenses.mapper

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.data.expenses.dto.AccountBriefDto
import com.yandex.school.casheye.data.expenses.dto.AccountDto
import com.yandex.school.casheye.data.expenses.dto.CategoryDto
import com.yandex.school.casheye.data.expenses.dto.TransactionResponseDto
import java.math.BigDecimal

internal fun AccountDto.toDomain(): Account =
    Account(
        id = id,
        name = name,
        emoji = emoji,
        balance = BigDecimal(balance),
        currency = currency,
    )

internal fun AccountBriefDto.toDomain(): Account =
    Account(
        id = id,
        name = name,
        emoji = emoji,
        balance = BigDecimal(balance),
        currency = currency,
    )

internal fun CategoryDto.toDomain(): Category =
    Category(
        id = id,
        name = name,
        emoji = emoji,
        isIncome = isIncome,
    )

internal fun TransactionResponseDto.toDomain(): Transaction =
    Transaction(
        id = id,
        account = account.toDomain(),
        category = category.toDomain(),
        amount = BigDecimal(amount),
        transactionDate = transactionDate,
        comment = comment,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

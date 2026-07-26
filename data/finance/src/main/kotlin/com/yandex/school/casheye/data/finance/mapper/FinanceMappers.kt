package com.yandex.school.casheye.data.finance.mapper

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.data.finance.dto.AccountBriefDto
import com.yandex.school.casheye.data.finance.dto.AccountDto
import com.yandex.school.casheye.data.finance.dto.AccountResponseDto
import com.yandex.school.casheye.data.finance.dto.CategoryDto
import com.yandex.school.casheye.data.finance.dto.TransactionResponseDto
import java.math.BigDecimal

internal fun AccountDto.toDomain(): Account =
    Account(
        id = id,
        name = name,
        emoji = emoji,
        balance = BigDecimal(balance),
        currency = CurrencyCode.fromIsoCode(currency),
    )

internal fun AccountBriefDto.toDomain(): Account =
    Account(
        id = id,
        name = name,
        emoji = emoji,
        balance = BigDecimal(balance),
        currency = CurrencyCode.fromIsoCode(currency),
    )

internal fun AccountResponseDto.toDomain(): Account =
    Account(
        id = id,
        name = name,
        emoji = emoji,
        balance = BigDecimal(balance),
        currency = CurrencyCode.fromIsoCode(currency),
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
        currency = CurrencyCode.fromIsoCode(account.currency),
        transactionDate = transactionDate,
        comment = comment,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

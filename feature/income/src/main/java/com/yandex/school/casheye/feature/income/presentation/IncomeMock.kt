package com.yandex.school.casheye.feature.income.presentation

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.core.model.Transaction
import java.math.BigDecimal
import java.time.Instant

private val MockDate: Instant = Instant.parse("2026-06-12T12:00:00Z")
private val MockAccount =
    Account(
        id = 1,
        name = "Основной счёт",
        emoji = "\uD83D\uDCB5",
        balance = BigDecimal("1322444"),
        currency = CurrencyCode.RUB,
    )

internal val incomeUiStateMock =
    IncomeUiState.Content(
        total = BigDecimal("323524"),
        currencyCode = CurrencyCode.RUB.isoCode,
        transactions =
            listOf(
                incomeTransaction(101, 201, "Продажа старой мебели", "🛋️", "8500"),
                incomeTransaction(102, 202, "Возврат налога", "📋", "15000"),
                incomeTransaction(103, 203, "Премия за проект", "💼", "25000"),
                incomeTransaction(104, 204, "Подработка фриланс", "💻", "13450"),
                incomeTransaction(105, 205, "Сдача квартиры", "🏠", "38000"),
                incomeTransaction(106, 206, "Кешбек на карту", "💳", "1200"),
                incomeTransaction(107, 207, "Подарок от родителей", "🎁", "5000"),
            ),
    )

private fun incomeTransaction(
    id: Int,
    categoryId: Int,
    categoryName: String,
    emoji: String,
    amount: String,
): Transaction =
    Transaction(
        id = id,
        account = MockAccount,
        category =
            Category(
                id = categoryId,
                name = categoryName,
                emoji = emoji,
                isIncome = true,
            ),
        amount = BigDecimal(amount),
        transactionDate = MockDate,
        comment = null,
        createdAt = MockDate,
        updatedAt = MockDate,
    )

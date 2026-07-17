package com.yandex.school.casheye.feature.expenses.presentation

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.Transaction
import java.math.BigDecimal
import java.time.Instant

private const val CURRENCY_RUB = "RUB"
private val MockDate: Instant = Instant.parse("2026-06-12T12:00:00Z")
private val MockAccount =
    Account(
        id = 1,
        name = "Основной счёт",
        emoji = "\uD83D\uDCB5",
        balance = BigDecimal("1322444"),
        currency = CURRENCY_RUB,
    )

internal val expensesUiStateMock =
    ExpensesUiState.Content(
        total = BigDecimal("323524"),
        currencyCode = CURRENCY_RUB,
        transactions =
            listOf(
                expenseTransaction(1, 101, "Покупка канцтоваров", "✏️", "1200"),
                expenseTransaction(2, 102, "Обед в кафе", "☕", "750"),
                expenseTransaction(3, 103, "Топливо для машины", "⛽", "2300"),
                expenseTransaction(4, 104, "Подписка на сервис", "📱", "450"),
                expenseTransaction(5, 105, "Ремонт техники", "🔧", "5800"),
                expenseTransaction(6, 106, "Покупка билетов", "🎫", "3200"),
                expenseTransaction(7, 107, "Оплата интернета", "🌐", "800"),
                expenseTransaction(8, 108, "Магазин продуктов", "🛒", "2450"),
            ),
    )

private fun expenseTransaction(
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
                isIncome = false,
            ),
        amount = BigDecimal(amount),
        transactionDate = MockDate,
        comment = null,
        createdAt = MockDate,
        updatedAt = MockDate,
    )

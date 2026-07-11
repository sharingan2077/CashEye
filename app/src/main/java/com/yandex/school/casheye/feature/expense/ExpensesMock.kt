package com.yandex.school.casheye.feature.expense

import com.yandex.school.casheye.domain.model.Account
import com.yandex.school.casheye.domain.model.Category
import com.yandex.school.casheye.domain.model.Transaction
import java.math.BigDecimal
import java.time.Instant

private const val CurrencyRub = "RUB"
private val MockDate: Instant = Instant.parse("2026-06-12T12:00:00Z")
private val MockAccount = Account(
    id = 1,
    name = "Основной счёт",
    balance = BigDecimal("1322444"),
    currency = CurrencyRub,
)

internal val expensesUiStateMock = ExpensesUiState(
    total = BigDecimal("323524"),
    currencyCode = CurrencyRub,
    transactions = listOf(
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
): Transaction = Transaction(
    id = id,
    account = MockAccount,
    category = Category(
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

package com.yandex.school.casheye.feature.analytics.presentation

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

internal val analyticsUiStateMock =
    AnalyticsUiState(
        total = BigDecimal("323524"),
        currencyCode = CURRENCY_RUB,
        filters =
            listOf(
                Filter.Type(type = "Расходы"),
                Filter.Period(period = "20.01.2026 – 05.02.2026"),
                Filter.Articles(articles = listOf("Ремонт", "Авто")),
                Filter.Account(accounts = listOf("Все счета")),
            ),
        transactions =
            listOf(
                analyticsTransaction(1, 101, "На собачку", comment = "Джек", "\uD83D\uDC36", "123322"),
                analyticsTransaction(
                    2,
                    102,
                    "Покупки в магазине",
                    comment = "Сбербанк",
                    "\uD83D\uDED2",
                    "45100",
                ),
            ),
    )

private fun analyticsTransaction(
    id: Int,
    categoryId: Int,
    categoryName: String,
    comment: String?,
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
        comment = comment,
        createdAt = MockDate,
        updatedAt = MockDate,
    )

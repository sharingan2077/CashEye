package com.yandex.school.casheye.data.analytics.local


import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.domain.analytics.model.Analytics
import com.yandex.school.casheye.domain.analytics.model.AnalyticsFilter
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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

class AnalyticsDaoMock @Inject constructor() : AnalyticsDao {
    override fun observeAnalytics(): Flow<Analytics> {
        return flowOf(AnalyticsMock)
    }
}

private val AnalyticsMock = Analytics(
    total = BigDecimal("323524"),
    currencyCode = CurrencyRub,
    filters = listOf(
        AnalyticsFilter.Type(type = "Расходы"),
        AnalyticsFilter.Period(period = "20.01.2026 – 05.02.2026"),
        AnalyticsFilter.Articles(articles = listOf("Ремонт", "Авто")),
        AnalyticsFilter.Account(accounts = listOf("Все счета")),

        ),
    transactions = listOf(
        analyticsTransaction(1, 101, "На собачку", comment = "Джек", "\uD83D\uDC36", "123322"),
        analyticsTransaction(
            2,
            102,
            "Покупки в магазине",
            comment = "Сбербанк",
            "\uD83D\uDED2",
            "45100"
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
    comment = comment,
    createdAt = MockDate,
    updatedAt = MockDate,
)

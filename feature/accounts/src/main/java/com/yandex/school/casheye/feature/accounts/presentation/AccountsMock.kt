package com.yandex.school.casheye.feature.accounts.presentation

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.core.model.MoneyAmount
import java.math.BigDecimal

internal val accountsUiStateMock =
    AccountsUiState.Content(
        nativeTotals = listOf(MoneyAmount(BigDecimal("1322444"), CurrencyCode.RUB)),
        currentValuation = null,
        accounts =
            listOf(
                accountItem(1, "Яндекс Pay", "💳", "123322"),
                accountItem(2, "Газпромбанк", "🏦", "122322"),
                accountItem(3, "Сбербанк", "🏦", "122322"),
            ),
    )

private fun accountItem(
    id: Int,
    name: String,
    emoji: String,
    balance: String,
): Account =
    Account(
        id = id,
        name = name,
        emoji = emoji,
        balance = BigDecimal(balance),
        currency = CurrencyCode.RUB,
    )

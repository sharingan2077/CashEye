package com.yandex.school.casheye.feature.accounts.presentation

import com.yandex.school.casheye.core.model.Account
import java.math.BigDecimal

private const val CURRENCY_RUB = "RUB"

internal val accountsUiStateMock =
    AccountsUiState(
        total = BigDecimal("1322444"),
        currencyCode = CURRENCY_RUB,
        accounts =
            listOf(
                accountItem(1, "Яндекс Pay", "123322", "💳"),
                accountItem(2, "Газпромбанк", "122322", "🏦"),
                accountItem(3, "Сбербанк", "122322", "🏦"),
            ),
    )

private fun accountItem(
    id: Int,
    name: String,
    balance: String,
    emoji: String,
): AccountListItemUi =
    AccountListItemUi(
        account =
            Account(
                id = id,
                name = name,
                balance = BigDecimal(balance),
                currency = CURRENCY_RUB,
            ),
        emoji = emoji,
    )

package com.yandex.school.casheye.feature.accounts.data.local

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.feature.accounts.domain.model.Accounts
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.math.BigDecimal
import javax.inject.Inject

private const val CurrencyRub = "RUB"
private val AccountsMock: Accounts = Accounts(
    total = BigDecimal("1322444"),
    currencyCode = CurrencyRub,
    accounts =
        listOf(
            Account(1, "Яндекс Pay", BigDecimal("123322"), "RUB"),
            Account(2, "Газпромбанк", BigDecimal("122322"), "RUB"),
            Account(3, "Сбербанк", BigDecimal("122322"), "RUB"),
        )
)

class AccountsDaoMock @Inject constructor() : AccountsDao {
    override fun observeAccounts(): Flow<Accounts> {
        return flowOf(AccountsMock)
    }
}
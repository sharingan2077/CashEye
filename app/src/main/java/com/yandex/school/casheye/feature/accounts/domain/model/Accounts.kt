package com.yandex.school.casheye.feature.accounts.domain.model

import com.yandex.school.casheye.core.model.Account
import java.math.BigDecimal


data class Accounts(
    val total: BigDecimal,
    val currencyCode: String,
    val accounts: List<Account>
)
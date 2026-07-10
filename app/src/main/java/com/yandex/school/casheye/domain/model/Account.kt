package com.yandex.school.casheye.domain.model

import java.math.BigDecimal

data class Account(
    val id: Int,
    val name: String,
    val balance: BigDecimal,
    val currency: String
)
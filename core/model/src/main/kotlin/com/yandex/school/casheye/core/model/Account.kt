package com.yandex.school.casheye.core.model

import java.math.BigDecimal

data class Account(
    val id: Int,
    val name: String,
    val emoji: String,
    val balance: BigDecimal,
    val currency: String,
)

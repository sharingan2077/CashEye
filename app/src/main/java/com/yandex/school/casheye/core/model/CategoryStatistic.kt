package com.yandex.school.casheye.core.model

import java.math.BigDecimal

data class CategoryStatistic(
    val categoryId: Int,
    val categoryName: String,
    val emoji: String,
    val amount: BigDecimal
)

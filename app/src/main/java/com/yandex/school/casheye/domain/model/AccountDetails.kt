package com.yandex.school.casheye.domain.model

import java.time.Instant

data class AccountDetails(
    val account: Account,
    val incomeStats: List<CategoryStatistic>,
    val expenseStats: List<CategoryStatistic>,
    val createdAt: Instant,
    val updatedAt: Instant
)
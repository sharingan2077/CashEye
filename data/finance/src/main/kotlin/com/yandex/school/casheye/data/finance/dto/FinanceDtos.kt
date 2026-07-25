package com.yandex.school.casheye.data.finance.dto

import com.yandex.school.casheye.data.finance.mapper.InstantIso8601Serializer
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class AccountDto(
    val id: Int,
    val userId: Int,
    val name: String,
    val emoji: String,
    val balance: String,
    val currency: String,
    @Serializable(with = InstantIso8601Serializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantIso8601Serializer::class)
    val updatedAt: Instant,
)

@Serializable
data class AccountBriefDto(
    val id: Int,
    val name: String,
    val emoji: String,
    val balance: String,
    val currency: String,
)

@Serializable
data class CategoryDto(
    val id: Int,
    val name: String,
    val emoji: String,
    val isIncome: Boolean,
)

@Serializable
data class TransactionResponseDto(
    val id: Int,
    val account: AccountBriefDto,
    val category: CategoryDto,
    val amount: String,
    @Serializable(with = InstantIso8601Serializer::class)
    val transactionDate: Instant,
    val comment: String?,
    @Serializable(with = InstantIso8601Serializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantIso8601Serializer::class)
    val updatedAt: Instant,
)

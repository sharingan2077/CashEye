package com.yandex.school.casheye.data.finance.network

import com.yandex.school.casheye.data.finance.dto.AccountBriefDto
import com.yandex.school.casheye.data.finance.dto.AccountDto
import com.yandex.school.casheye.data.finance.dto.AccountResponseDto
import com.yandex.school.casheye.data.finance.dto.CategoryDto
import com.yandex.school.casheye.data.finance.dto.TransactionResponseDto
import com.yandex.school.casheye.data.finance.mapper.toDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class ExpensesMappersTest {
    @Test
    fun `account response maps editor account`() {
        val account = AccountResponseDto(3, "Резерв", "💳", "99.50", "USD").toDomain()

        assertEquals(3, account.id)
        assertEquals(BigDecimal("99.50"), account.balance)
        assertEquals("USD", account.currency)
    }

    @Test
    fun `account dto maps string balance to domain amount`() {
        val account =
            AccountDto(
                id = 7,
                userId = 3,
                name = "Основной счёт",
                emoji = "\uD83D\uDCB5",
                balance = "1250.75",
                currency = "RUB",
                createdAt = Instant.parse("2026-07-17T08:00:00Z"),
                updatedAt = Instant.parse("2026-07-17T09:00:00Z"),
            ).toDomain()

        assertEquals(7, account.id)
        assertEquals("Основной счёт", account.name)
        assertEquals(BigDecimal("1250.75"), account.balance)
        assertEquals("RUB", account.currency)
    }

    @Test
    fun `transaction dto maps nullable comment and iso instants`() {
        val transaction = transactionDto(comment = null).toDomain()

        assertEquals(BigDecimal("42.10"), transaction.amount)
        assertEquals(Instant.parse("2026-07-17T12:34:56Z"), transaction.transactionDate)
        assertEquals(Instant.parse("2026-07-17T12:35:00Z"), transaction.createdAt)
        assertEquals(Instant.parse("2026-07-17T12:36:00Z"), transaction.updatedAt)
        assertNull(transaction.comment)
        assertEquals("Основной счёт", transaction.account.name)
        assertEquals(BigDecimal("1250.75"), transaction.account.balance)
        assertEquals("Продукты", transaction.category.name)
        assertFalse(transaction.category.isIncome)
    }
}

private fun transactionDto(comment: String?): TransactionResponseDto =
    TransactionResponseDto(
        id = 11,
        account =
            AccountBriefDto(
                id = 7,
                name = "Основной счёт",
                emoji = "\uD83D\uDCB5",
                balance = "1250.75",
                currency = "RUB",
            ),
        category =
            CategoryDto(
                id = 5,
                name = "Продукты",
                emoji = "🛒",
                isIncome = false,
            ),
        amount = "42.10",
        transactionDate = Instant.parse("2026-07-17T12:34:56Z"),
        comment = comment,
        createdAt = Instant.parse("2026-07-17T12:35:00Z"),
        updatedAt = Instant.parse("2026-07-17T12:36:00Z"),
    )

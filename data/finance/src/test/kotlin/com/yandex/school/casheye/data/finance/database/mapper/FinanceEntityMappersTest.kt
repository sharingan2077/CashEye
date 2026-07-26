package com.yandex.school.casheye.data.finance.database.mapper

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.data.finance.database.entity.TransactionWithRelations
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class FinanceEntityMappersTest {
    @Test
    fun `account keeps exact decimal representation`() {
        val account =
            Account(
                id = 7,
                name = "Savings",
                emoji = "💰",
                balance = BigDecimal("1234567890.0010"),
                currency = "RUB",
            )

        val restored = account.toEntity().toDomain()

        assertEquals(account, restored)
        assertEquals("1234567890.0010", account.toEntity().balance)
    }

    @Test
    fun `transaction keeps money dates and relations`() {
        val account = Account(7, "Main", "💳", BigDecimal("42.10"), "RUB")
        val category = Category(9, "Food", "🍜", false)
        val transaction =
            Transaction(
                id = 11,
                account = account,
                category = category,
                amount = BigDecimal("0.0100"),
                transactionDate = Instant.parse("2026-07-22T10:15:30.123Z"),
                comment = "Lunch",
                createdAt = Instant.parse("2026-07-22T10:16:00.456Z"),
                updatedAt = Instant.parse("2026-07-22T11:17:00.789Z"),
            )
        val entity = transaction.toEntity()

        val restored =
            TransactionWithRelations(
                transaction = entity,
                account = account.toEntity(),
                category = category.toEntity(),
            ).toDomain()

        assertEquals(transaction, restored)
        assertEquals("0.0100", entity.amount)
        assertEquals(transaction.transactionDate.toEpochMilli(), entity.transactionDate)
        assertEquals(account.id, entity.accountId)
        assertEquals(category.id, entity.categoryId)
    }

    @Test
    fun `transaction relation uses the newly selected account currency`() {
        val rubAccount = Account(7, "Roubles", "💳", BigDecimal.ZERO, CurrencyCode.RUB)
        val usdAccount = Account(8, "Dollars", "💵", BigDecimal.ZERO, CurrencyCode.USD)
        val category = Category(9, "Food", "🍜", false)
        val transaction =
            Transaction(
                id = 11,
                account = rubAccount,
                category = category,
                amount = BigDecimal.TEN,
                transactionDate = Instant.EPOCH,
                comment = null,
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            )

        val restored =
            TransactionWithRelations(
                transaction = transaction.toEntity().copy(accountId = usdAccount.id),
                account = usdAccount.toEntity(),
                category = category.toEntity(),
            ).toDomain()

        assertEquals(CurrencyCode.USD, restored.currency)
    }
}

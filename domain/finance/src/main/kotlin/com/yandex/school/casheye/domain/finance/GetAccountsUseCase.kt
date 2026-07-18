package com.yandex.school.casheye.domain.finance

class GetAccountsUseCase(
    private val repository: FinanceRepository,
) {
    suspend operator fun invoke(currencyCode: String): AccountsLoadResult = repository.getAccountsSummary(currencyCode)
}

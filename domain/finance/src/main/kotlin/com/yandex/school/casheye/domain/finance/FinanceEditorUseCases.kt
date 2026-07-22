package com.yandex.school.casheye.domain.finance

class GetEditorAccountsUseCase(
    private val repository: FinanceRepository,
) {
    suspend operator fun invoke() = repository.getAccounts()
}

class GetEditorCategoriesUseCase(
    private val repository: FinanceRepository,
) {
    suspend operator fun invoke(isIncome: Boolean) = repository.getCategories(isIncome)
}

class GetTransactionUseCase(
    private val repository: FinanceRepository,
) {
    suspend operator fun invoke(id: Int) = repository.getTransaction(id)
}

class SaveTransactionUseCase(
    private val repository: FinanceRepository,
) {
    suspend operator fun invoke(command: SaveTransactionCommand) = repository.saveTransaction(command)
}

class GetAccountUseCase(
    private val repository: FinanceRepository,
) {
    suspend operator fun invoke(id: Int) = repository.getAccount(id)
}

class SaveAccountUseCase(
    private val repository: FinanceRepository,
) {
    suspend operator fun invoke(command: SaveAccountCommand) = repository.saveAccount(command)
}

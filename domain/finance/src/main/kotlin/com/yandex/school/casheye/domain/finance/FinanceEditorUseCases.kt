package com.yandex.school.casheye.domain.finance

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.domain.finance.editor.EditorResult
import com.yandex.school.casheye.domain.finance.editor.SaveAccountCommand
import com.yandex.school.casheye.domain.finance.editor.SaveTransactionCommand

class GetEditorAccountsUseCase(
    private val repository: FinanceQueryRepository,
) {
    suspend operator fun invoke(): EditorResult<List<Account>> =
        when (val result = repository.getAccounts()) {
            is FinanceDataLoadResult.Success -> EditorResult.Success(result.data)
            is FinanceDataLoadResult.Failure -> EditorResult.Failure(result.reason)
        }
}

class GetEditorCategoriesUseCase(
    private val repository: FinanceEditorRepository,
) {
    suspend operator fun invoke(isIncome: Boolean) = repository.getCategories(isIncome)
}

class GetTransactionUseCase(
    private val repository: FinanceEditorRepository,
) {
    suspend operator fun invoke(id: Int) = repository.getTransaction(id)
}

class SaveTransactionUseCase(
    private val repository: FinanceEditorRepository,
) {
    suspend operator fun invoke(command: SaveTransactionCommand) = repository.saveTransaction(command)
}

class DeleteTransactionUseCase(
    private val repository: FinanceEditorRepository,
) {
    suspend operator fun invoke(id: Int) = repository.deleteTransaction(id)
}

class GetAccountUseCase(
    private val repository: FinanceEditorRepository,
) {
    suspend operator fun invoke(id: Int) = repository.getAccount(id)
}

class SaveAccountUseCase(
    private val repository: FinanceEditorRepository,
) {
    suspend operator fun invoke(command: SaveAccountCommand) = repository.saveAccount(command)
}

class GetAccountTransactionCountUseCase(
    private val repository: FinanceEditorRepository,
) {
    suspend operator fun invoke(id: Int) = repository.getAccountTransactionCount(id)
}

class DeleteAccountUseCase(
    private val repository: FinanceEditorRepository,
) {
    suspend operator fun invoke(id: Int) = repository.deleteAccount(id)
}

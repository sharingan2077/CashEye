package com.yandex.school.casheye.domain.finance

sealed interface FinanceDataLoadResult<out T> {
    data class Success<T>(
        val data: T,
    ) : FinanceDataLoadResult<T>

    data class Failure(
        val reason: FinanceFailureReason,
    ) : FinanceDataLoadResult<Nothing>
}

sealed interface FinanceRefreshResult {
    data object Success : FinanceRefreshResult

    data class Failure(
        val reason: FinanceFailureReason,
        val hasUsableCache: Boolean,
    ) : FinanceRefreshResult
}

sealed interface FinanceFailureReason {
    data object Network : FinanceFailureReason

    data object Authorization : FinanceFailureReason

    data object Server : FinanceFailureReason

    data object Unknown : FinanceFailureReason
}

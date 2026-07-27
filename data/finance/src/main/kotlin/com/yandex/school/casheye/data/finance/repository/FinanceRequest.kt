package com.yandex.school.casheye.data.finance.repository

import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.domain.finance.editor.EditorResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

internal suspend inline fun <T> editorRequest(
    dispatcher: CoroutineDispatcher,
    crossinline block: suspend () -> T,
): EditorResult<T> =
    try {
        EditorResult.Success(withContext(dispatcher) { block() })
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        EditorResult.Failure(error.toFailureReason())
    }

internal fun Exception.toFailureReason(): FinanceFailureReason =
    when (this) {
        is IOException -> {
            FinanceFailureReason.Network
        }

        is HttpException -> {
            when (code()) {
                HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN -> FinanceFailureReason.Authorization
                in HttpStatus.SERVER_ERROR_MIN..HttpStatus.SERVER_ERROR_MAX -> FinanceFailureReason.Server
                else -> FinanceFailureReason.Unknown
            }
        }

        else -> {
            FinanceFailureReason.Unknown
        }
    }

private object HttpStatus {
    const val UNAUTHORIZED = 401
    const val FORBIDDEN = 403
    const val SERVER_ERROR_MIN = 500
    const val SERVER_ERROR_MAX = 599
}

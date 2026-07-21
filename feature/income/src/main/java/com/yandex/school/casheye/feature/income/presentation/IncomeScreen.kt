package com.yandex.school.casheye.feature.income.presentation

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yandex.school.casheye.core.designsystem.component.MoneyListItem
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import com.yandex.school.casheye.core.format.formatAmount
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.feature.income.R

@Composable
fun IncomeScreen(
    state: IncomeUiState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        when (state) {
            IncomeUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            IncomeUiState.Empty -> {
                EmptyIncome(modifier = Modifier.align(Alignment.Center))
            }

            is IncomeUiState.Content -> {
                IncomeContent(state = state)
            }

            is IncomeUiState.Error -> {
                IncomeError(
                    message = state.reason.localizedMessage(),
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun IncomeContent(
    state: IncomeUiState.Content,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize(),
    ) {
        IncomeHero(
            total =
                formatAmount(
                    amount = state.total,
                    currencyCode = state.currencyCode,
                ),
        )
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
        ) {
            items(
                items = state.transactions,
                key = Transaction::id,
            ) { transaction ->
                MoneyListItem(
                    emoji = transaction.category.emoji,
                    title = transaction.category.name,
                    amount =
                        formatAmount(
                            amount = transaction.amount,
                            currencyCode = transaction.account.currency,
                        ),
                )
            }
        }
    }
}

@Composable
private fun EmptyIncome(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.image_empty_income),
            contentDescription = stringResource(R.string.empty_income),
            modifier =
                Modifier
                    .size(200.dp),
        )
        Text(
            text = stringResource(R.string.empty_income),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.empty_income_description),
            modifier = Modifier.widthIn(max = 280.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun IncomeError(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun IncomeHero(total: String) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(117.dp)
                .padding(start = 20.dp, top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.income_total),
            style = MaterialTheme.typography.labelLarge.copy(lineHeight = 16.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Text(
            text = total,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun FinanceFailureReason.localizedMessage(): String =
    stringResource(
        when (this) {
            FinanceFailureReason.Network -> R.string.error_network
            FinanceFailureReason.Authorization -> R.string.error_authorization
            FinanceFailureReason.Server -> R.string.error_server
            FinanceFailureReason.Unknown -> R.string.error_load_income
        },
    )

@Preview(
    name = "Light",
    showBackground = true,
    widthDp = 412,
    heightDp = 892,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "Dark",
    showBackground = true,
    widthDp = 412,
    heightDp = 892,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun IncomeScreenPreview() {
    CashEyeTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            IncomeScreen(state = incomeUiStateMock)
        }
    }
}

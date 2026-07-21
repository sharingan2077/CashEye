package com.yandex.school.casheye.feature.accounts.presentation

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
import com.yandex.school.casheye.core.designsystem.component.ErrorState
import com.yandex.school.casheye.core.designsystem.component.ErrorStateType
import com.yandex.school.casheye.core.designsystem.component.MoneyListItem
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import com.yandex.school.casheye.core.format.formatAmount
import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.feature.accounts.R

@Composable
fun AccountsScreen(
    state: AccountsUiState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        when (state) {
            AccountsUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            AccountsUiState.Empty -> {
                EmptyAccounts(modifier = Modifier.align(Alignment.Center))
            }

            is AccountsUiState.Content -> {
                AccountsContent(state = state)
            }

            is AccountsUiState.Error -> {
                ErrorState(type = state.reason.toErrorStateType())
            }
        }
    }
}

@Composable
private fun AccountsContent(
    state: AccountsUiState.Content,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        AccountsHero(
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
                items = state.accounts,
                key = Account::id,
            ) { accountItem ->
                MoneyListItem(
                    emoji = accountItem.emoji,
                    title = accountItem.name,
                    amount =
                        formatAmount(
                            amount = accountItem.balance,
                            currencyCode = accountItem.currency,
                        ),
                )
            }
        }
    }
}

@Composable
private fun EmptyAccounts(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.image_empty_accounts),
            contentDescription = stringResource(R.string.empty_accounts),
            modifier =
                Modifier
                    .size(200.dp),
        )
        Text(
            text = stringResource(R.string.empty_accounts),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.empty_accounts_description),
            modifier = Modifier.widthIn(max = 280.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AccountsHero(total: String) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(117.dp)
                .padding(start = 20.dp, top = 12.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.balance_total),
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

private fun FinanceFailureReason.toErrorStateType(): ErrorStateType =
    when (this) {
        FinanceFailureReason.Network -> ErrorStateType.Network
        FinanceFailureReason.Authorization -> ErrorStateType.Authorization
        FinanceFailureReason.Server -> ErrorStateType.Server
        FinanceFailureReason.Unknown -> ErrorStateType.Unknown
    }

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
private fun AccountsScreenPreview() {
    CashEyeTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            AccountsScreen(state = accountsUiStateMock)
        }
    }
}

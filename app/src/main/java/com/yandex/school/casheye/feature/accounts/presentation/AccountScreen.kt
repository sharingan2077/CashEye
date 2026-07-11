package com.yandex.school.casheye.feature.accounts.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yandex.school.casheye.core.designsystem.component.MoneyListItem
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import com.yandex.school.casheye.core.format.formatAmount
import com.yandex.school.casheye.core.model.Account
import java.math.BigDecimal

data class AccountsUiState(
    val total: BigDecimal,
    val currencyCode: String,
    val accounts: List<AccountListItemUi>,
)

data class AccountListItemUi(
    val account: Account,
    val emoji: String,
)

@Composable
fun AccountScreen(
    state: AccountsUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        AccountHero(
            total = formatAmount(
                amount = state.total,
                currencyCode = state.currencyCode,
            ),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            items(
                items = state.accounts,
                key = { it.account.id },
            ) { accountItem ->
                MoneyListItem(
                    emoji = accountItem.emoji,
                    title = accountItem.account.name,
                    amount = formatAmount(
                        amount = accountItem.account.balance,
                        currencyCode = accountItem.account.currency,
                    ),
                )
            }
        }
    }
}

@Composable
private fun AccountHero(total: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(117.dp)
            .padding(start = 20.dp, top = 12.dp),
    ) {
        Text(
            text = "баланс, всего",
            style = MaterialTheme.typography.labelLarge.copy(lineHeight = 16.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Text(
            text = total,
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun AccountScreenPreview() {
    CashEyeTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            AccountScreen(state = accountsUiStateMock)
        }
    }
}

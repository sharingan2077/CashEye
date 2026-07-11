package com.yandex.school.casheye.feature.income

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yandex.school.casheye.ui.theme.CashEyeTheme

@Immutable
private data class IncomeItem(
    val emoji: String,
    val title: String,
    val amount: String
)

private val incomes = listOf<IncomeItem>(
    IncomeItem("\uD83D\uDECB", "Продажа старой мебели", "8 500 ₽"),
    IncomeItem("\uD83D\uDCCB", "Возврат налога", "15 000 ₽"),
    IncomeItem("\uD83D\uDCBC", "Премия за проект", "25 000 ₽"),
    IncomeItem("\uD83D\uDCBB", "Подработка фриланс", "13 450 ₽"),
    IncomeItem("\uD83C\uDFE0", "Сдача квартиры", "38 000 ₽"),
    IncomeItem("\uD83D\uDCB3", "Кешбек на карту", "1 200 ₽"),
    IncomeItem("\uD83C\uDF81", "Подарок от родителей", "5 000 ₽"),
)

@Composable
fun IncomeScreen(modifier: Modifier = Modifier) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        IncomeHero()

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {

            items(incomes) { income ->
                ListItem(income = income)
            }

        }
    }
}

@Composable
fun IncomeHero() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(117.dp)
            .padding(start = 20.dp, top = 12.dp)
    ) {
        Text(
            text = "доходы, всего",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text = "323 524 ₽",
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


@Composable
private fun ListItem(
    income: IncomeItem
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically

    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = income.emoji,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = income.title,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )

        Text(
            text = income.amount,
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.labelLarge,
        )
    }

}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun IncomeScreenPreview() {
    CashEyeTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            IncomeScreen()
        }
    }
}

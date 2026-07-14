package com.yandex.school.casheye.feature.analytics.presentaion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.school.casheye.R
import com.yandex.school.casheye.core.designsystem.component.EmojiCircle
import com.yandex.school.casheye.core.designsystem.component.ListItem
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import com.yandex.school.casheye.core.format.formatAmount


@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AnalyticsContent(
        state = state,
        modifier = modifier
    )


}

@Composable
fun AnalyticsContent(
    state: AnalyticsUiState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
    ) {
        item {
            AnalyticsView(
                total = formatAmount(
                    amount = state.total,
                    currencyCode = state.currencyCode
                ),
                articles = mapOf(
                    "Ремонт" to 50000,
                    "Авто" to 45000,
                    "Другое" to 30000

                )
            )
        }

        item {
            FilterView(
                filters = state.filters
            )
        }

        item {
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 8.dp)
                    .wrapContentHeight()
                    .wrapContentWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Транзакции",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 22.sp,
                    )
                )
            }
        }

        items(items = state.transactions, key = { it.id }) { transaction ->
            Column {
                TransactionItem(
                    emoji = transaction.category.emoji,
                    title = transaction.category.name,
                    comment = transaction.comment,
                    amount = formatAmount(
                        amount = transaction.amount,
                        currencyCode = "RUB"
                    )
                )
                Spacer(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.outline)
                )
            }
        }
    }
}

@Composable
fun TransactionItem(
    emoji: String,
    title: String,
    comment: String?,
    amount: String,
) {

    ListItem(
        lead = {
            EmojiCircle(
                emoji = emoji
            )

        },
        content = {

            Column(
                modifier = Modifier,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                if (comment != null) {
                    Text(
                        text = comment,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                }
            }
        },
        trail = {
            Text(
                text = amount,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.End,
            )

        },
    )

}


@Composable
fun FilterView(
    filters: List<Filter>
) {
    Column(
        modifier = Modifier
            .padding(vertical = 8.dp)
    ) {
        filters.forEach { filter ->
            when (filter) {
                is Filter.Type -> {
                    AnalyticsFilterItem(
                        iconPainter = painterResource(filter.resId),
                        contentDescription = filter.title,
                        title = filter.title,
                        filter = filter.type
                    )
                }

                is Filter.Period -> {
                    AnalyticsFilterItem(
                        iconPainter = painterResource(filter.resId),
                        contentDescription = filter.title,
                        title = filter.title,
                        filter = filter.period
                    )

                }

                is Filter.Articles -> {
                    AnalyticsFilterItem(
                        iconPainter = painterResource(R.drawable.tag),
                        contentDescription = filter.title,
                        title = filter.title,
                        filter = filter.articles.toString()
                    )

                }

                is Filter.Account -> {
                    AnalyticsFilterItem(
                        iconPainter = painterResource(R.drawable.credit_card),
                        contentDescription = filter.title,
                        title = filter.title,
                        filter = filter.accounts.toString()
                    )
                }
            }
            Spacer(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.outline)
            )
        }
    }
}

@Composable
fun AnalyticsFilterItem(
    iconPainter: Painter,
    contentDescription: String?,
    title: String,
    filter: String
) {

    ListItem(
        lead = {
            IconCircle(
                iconPainter = iconPainter,
                contentDescription = contentDescription
            )
        },
        content = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trail = {
            FilterItem(
                title = filter
            )
        },
        height = 56.dp
    )

}

@Composable
fun FilterItem(
    title: String
) {
    Box(
        modifier = Modifier
            .height(24.dp)
            .wrapContentWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun IconCircle(
    iconPainter: Painter,
    contentDescription: String?
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = iconPainter,
            contentDescription = contentDescription
        )
    }

}

@Composable
private fun AnalyticsView(
    total: String,
    articles: Map<String, Int>,
    modifier: Modifier = Modifier
) {

    AnalyticsPieChart(
        total = total,
        articles = articles,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun AnalyticsScreenPreview() {
    CashEyeTheme(dynamicColor = false) {
        AnalyticsContent(
            state = AnalyticsUiState()
        )
    }
}

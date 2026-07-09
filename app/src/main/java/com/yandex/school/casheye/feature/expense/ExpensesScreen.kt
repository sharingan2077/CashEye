package com.yandex.school.casheye.feature.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yandex.school.casheye.ui.theme.CashEyeTheme

@Immutable
private data class ExpenseItem(
    val emoji: String,
    val title: String,
    val amount: String,
)

private val Expenses = listOf(
    ExpenseItem(emoji = "✏️", title = "Покупка канцтоваров", amount = "1 200 ₽"),
    ExpenseItem(emoji = "☕", title = "Обед в кафе", amount = "750 ₽"),
    ExpenseItem(emoji = "⛽", title = "Топливо для машины", amount = "2 300 ₽"),
    ExpenseItem(emoji = "📱", title = "Подписка на сервис", amount = "450 ₽"),
    ExpenseItem(emoji = "🔧", title = "Ремонт техники", amount = "5 800 ₽"),
    ExpenseItem(emoji = "🎫", title = "Покупка билетов", amount = "3 200 ₽"),
    ExpenseItem(emoji = "🌐", title = "Оплата интернета", amount = "800 ₽"),
    ExpenseItem(emoji = "🛒", title = "Магазин продуктов", amount = "2 450 ₽"),
)

@Composable
fun ExpenseScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = { ExpensesTopBar() },
        floatingActionButton = { ExpensesFab() },
        bottomBar = { ExpensesBottomBar() },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            ExpensesHero()
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                items(Expenses) { expense ->
                    ExpenseRow(expense = expense)
                }
            }
        }
    }
}

@Composable
private fun ExpensesTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 12.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        DateFilterButton()
        Row(
            modifier = Modifier.height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TopBarIcon(icon = Icons.Outlined.Analytics, contentDescription = "Аналитика")
            TopBarIcon(icon = Icons.Outlined.Tune, contentDescription = "Фильтры")
        }
    }
}

@Composable
private fun DateFilterButton() {
    Row(
        modifier = Modifier
            .height(48.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.CalendarMonth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = "Июль",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun TopBarIcon(
    icon: ImageVector,
    contentDescription: String,
) {
    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun ExpensesHero() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(117.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 20.dp, top = 12.dp),
    ) {
        Text(
            text = "расходы, всего",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = "323 524 ₽",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun ExpenseRow(expense: ExpenseItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = expense.emoji,
                fontSize = 22.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = expense.title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, end = 16.dp),
        )
        Text(
            text = expense.amount,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun ExpensesFab() {
    FloatingActionButton(
        onClick = {},
        modifier = Modifier.size(56.dp),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(16.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Добавить расход",
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun ExpensesBottomBar() {
    NavigationBar(
        modifier = Modifier.height(80.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
    ) {
        val items = listOf(
            BottomNavItem("Расходы", Icons.Outlined.ReceiptLong, selected = true),
            BottomNavItem("Доходы", Icons.Outlined.TrendingUp),
            BottomNavItem("Счета", Icons.Outlined.Person),
        )
        items.forEach { item ->
            NavigationBarItem(
                selected = item.selected,
                onClick = {},
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp),
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

private data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean = false,
)

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ExpenseScreenPreview() {
    CashEyeTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ExpenseScreen()
        }
    }
}

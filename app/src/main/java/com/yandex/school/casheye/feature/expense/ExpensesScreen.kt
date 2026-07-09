package com.yandex.school.casheye.feature.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yandex.school.casheye.ui.theme.CashEyeTheme

private val PrimaryGreen = Color(0xFF2AE881)
private val SummaryGreen = Color(0xFFD4FAE6)
private val SelectedGreen = Color(0xFFD4FAE6)
private val EmojiBackground = Color(0xFFD4FAE6)
private val TextPrimary = Color(0xFF1D1B20)
private val TextSecondary = Color(0xFF49454F)
private val DividerColor = Color(0xFFECE6F0)
private val ScreenBackground = Color(0xFFFFFBFE)

@Immutable
private data class ExpenseItem(
    val icon: String,
    val title: String,
    val subtitle: String? = null,
    val amount: String,
)

private val MockExpenses = listOf(
    ExpenseItem(icon = "🏠", title = "Аренда квартиры", amount = "100 000 ₽"),
    ExpenseItem(icon = "👕", title = "Одежда", amount = "15 000 ₽"),
    ExpenseItem(icon = "🐶", title = "На собачку", amount = "1 000 ₽"),
    ExpenseItem(icon = "🛠", title = "Ремонт квартиры", amount = "100 000 ₽"),
    ExpenseItem(icon = "🍔", title = "Продукты", amount = "15 000 ₽"),
    ExpenseItem(icon = "🏋", title = "Спортзал", amount = "15 000 ₽"),
    ExpenseItem(icon = "💊", title = "Медицина", amount = "15 000 ₽"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ScreenBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Расходы сегодня",
                        modifier = Modifier.fillMaxWidth(),
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Medium,
                    )
                },
                actions = {
                    Text(
                        text = "↺",
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        fontSize = 24.sp,
                        lineHeight = 24.sp,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(24.dp),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryGreen,
                    titleContentColor = TextPrimary,
                    actionIconContentColor = TextPrimary,
                ),
            )
        },
        bottomBar = { ExpensesNavigationBar() },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {},
                containerColor = PrimaryGreen,
                contentColor = TextPrimary,
                shape = CircleShape,
            ) {
                Text(
                    text = "+",
                    fontSize = 28.sp,
                    lineHeight = 28.sp,
                    textAlign = TextAlign.Center,
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ScreenBackground),
        ) {
            item {
                TotalRow(total = "261 000 ₽")
            }
            items(MockExpenses) { item ->
                ExpenseRow(item = item)
            }
        }
    }
}

@Composable
private fun TotalRow(total: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(SummaryGreen)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Всего",
            color = TextPrimary,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Normal,
        )
        Text(
            text = total,
            color = TextPrimary,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Normal,
        )
    }
    HorizontalDivider(color = DividerColor, thickness = DividerDefaults.Thickness)
}

@Composable
private fun ExpenseRow(item: ExpenseItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(ScreenBackground)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(EmojiBackground),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.icon,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, end = 8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = item.title,
                color = TextPrimary,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Normal,
            )
            item.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
        }
        Text(
            text = item.amount,
            color = TextPrimary,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.End,
        )
        Text(
            text = "›",
            color = TextSecondary,
            fontSize = 28.sp,
            lineHeight = 28.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(start = 16.dp)
                .size(24.dp),
        )
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        color = DividerColor,
        thickness = DividerDefaults.Thickness,
    )
}

@Composable
private fun ExpensesNavigationBar() {
    NavigationBar(
        containerColor = ScreenBackground,
        tonalElevation = 0.dp,
    ) {
        val items = listOf(
            NavigationItem("Расходы", "↓", selected = true),
            NavigationItem("Доходы", "↑"),
            NavigationItem("Счет", "▣"),
            NavigationItem("Статьи", "□"),
            NavigationItem("Настройки", "⚙"),
        )
        items.forEach { item ->
            NavigationBarItem(
                selected = item.selected,
                onClick = {},
                icon = {
                    Text(
                        text = item.icon,
                        fontSize = 24.sp,
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.size(24.dp),
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 1,
                    )
                },
                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                    selectedIconColor = TextPrimary,
                    selectedTextColor = TextPrimary,
                    indicatorColor = SelectedGreen,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                ),
            )
        }
    }
}

private data class NavigationItem(
    val label: String,
    val icon: String,
    val selected: Boolean = false,
)

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun ExpenseScreenPreview() {
    CashEyeTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ExpenseScreen()
        }
    }
}

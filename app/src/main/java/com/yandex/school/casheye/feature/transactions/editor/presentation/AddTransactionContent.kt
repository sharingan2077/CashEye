package com.yandex.school.casheye.feature.transactions.editor.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.R
import com.yandex.school.casheye.core.designsystem.component.AmountInput
import com.yandex.school.casheye.core.designsystem.component.FilterItem
import com.yandex.school.casheye.core.designsystem.component.IconFilterCircle
import com.yandex.school.casheye.core.designsystem.component.ListItem
import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import java.time.LocalDate
import java.time.LocalTime

sealed interface AddTransactionAction {

    data class AmountChanged(
        val value: String,
    ) : AddTransactionAction

    data class CategorySelected(
        val category: Category,
    ) : AddTransactionAction

    data class DateSelected(
        val date: LocalDate,
    ) : AddTransactionAction

    data class TimeSelected(
        val time: LocalTime,
    ) : AddTransactionAction

    data class AccountSelected(
        val account: Account,
    ) : AddTransactionAction

    data object ConfirmClicked : AddTransactionAction
}

enum class TransactionType {
    EXPENSE,
    INCOME
}


@Composable
fun AddTransactionContent(
    state: AddTransactionUiState,
    onAction: (AddTransactionAction) -> Unit,
    onCategoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Spacer(modifier = Modifier.height(24.dp))

        AmountInput(
            value = state.amount,
            onValueChange = {
                onAction(AddTransactionAction.AmountChanged(it))
            },
            modifier = Modifier.height(80.dp),
        )

        AddTransactionField(
            iconPainter = painterResource(R.drawable.article_purple),
            title = "Статья",
            value = state.selectedCategory?.name ?: "Выбрать",
            onClick = {
                onCategoryClick()
                // Показать выбор из state.availableCategories
            },
        )

        AddTransactionField(
            iconPainter = painterResource(R.drawable.calendar_purple),
            title = "Дата",
            value = state.selectedDate.toString(),
            onClick = {
                // Открыть DatePicker
            },
        )

        AddTransactionField(
            iconPainter = painterResource(R.drawable.calendar_purple),
            title = "Время",
            value = state.selectedTime.toString(),
            onClick = {
                // Открыть TimePicker
            },
        )

        AddTransactionField(
            iconPainter = painterResource(R.drawable.credit_purple),
            title = "Счёт",
            value = state.selectedAccount?.name ?: "Выбрать",
            onClick = {
                // Показать выбор из state.availableAccounts
            },
        )

        FloatingActionButton(
            onClick = {
                onAction(AddTransactionAction.ConfirmClicked)
            },
            containerColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.End)
                .padding(end = 16.dp, bottom = 9.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.check),
                tint = Color.White,
                contentDescription = "Добавить",
            )
        }
    }
}

@Composable
fun AddTransactionField(
    iconPainter: Painter,
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Column {
        ListItem(
            modifier = Modifier.clickable(onClick = onClick),
            lead = {
                IconFilterCircle(
                    iconPainter = iconPainter,
                    contentDescription = title,
                )
            },
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            trail = {
                FilterItem(title = value)
            },
            height = 64.dp,
        )

        Spacer(
            modifier = Modifier
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline),

            )
    }
}
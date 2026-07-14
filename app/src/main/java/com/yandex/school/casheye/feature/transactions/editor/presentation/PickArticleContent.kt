package com.yandex.school.casheye.feature.transactions.editor.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.core.designsystem.component.EmojiCircle
import com.yandex.school.casheye.core.designsystem.component.ListItem
import com.yandex.school.casheye.core.model.Category


@Composable
fun PickArticleContent(
    state: AddTransactionUiState,
    onApply: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {

    val initialCategoryId =
        state.selectedCategory?.id
            ?: state.availableCategories.firstOrNull()?.id

    var selectedCategoryId by remember(initialCategoryId) {
        mutableStateOf(initialCategoryId)
    }

    val selectedCategory = state.availableCategories.firstOrNull {
        it.id == selectedCategoryId
    }

    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Text(
            text = "Статьи",
            style = MaterialTheme.typography.headlineMedium
        )

//        val radioOptions = state.availableCategories.map { category -> category.name }
//        val (selectedOption, onOptionSelected) = remember { mutableStateOf(radioOptions[0]) }

        LazyColumn {
            items(
                items = state.availableCategories,
                key = { category -> category.id }) { category ->
                ArticleItem(
                    emoji = category.emoji,
                    title = category.name,
                    isSelected = selectedCategoryId == category.id,
                    onClick = {
                        selectedCategoryId = category.id
                    }

                )
            }
        }
        Box(
            modifier = Modifier
                .height(108.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = {
                    selectedCategory?.let(onApply)
                }
            ) {
                Text(
                    text = "Применить"
                )
            }

        }
    }
}

@Composable
fun ArticleItem(
    emoji: String,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {

    ListItem(
        lead = {

            EmojiCircle(
                emoji = emoji,
                size = 32.dp
            )
        },
        content = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
        },
        trail = {

            RadioButton(
                selected = isSelected,
                onClick = {
                    onClick()
                }
            )
        }
    )
}
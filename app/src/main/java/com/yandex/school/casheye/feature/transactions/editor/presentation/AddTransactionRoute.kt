package com.yandex.school.casheye.feature.transactions.editor.presentation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionRoute(
    type: TransactionType,
    onSaved: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var isCategorySheetVisible by rememberSaveable {
        mutableStateOf(false)
    }

    val categorySheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    LaunchedEffect(type) {
        viewModel.start(type)
    }

    AddTransactionContent(
        state = state,
        onAction = viewModel::onAction,
        onCategoryClick = {
            isCategorySheetVisible = true
        }
    )

    if (isCategorySheetVisible) {
        ModalBottomSheet(
            sheetState = categorySheetState,
            onDismissRequest = {
                isCategorySheetVisible = false
            }
        ) {
            PickArticleContent(
                state = state,
                onApply = { category ->
                    viewModel.onAction(
                        AddTransactionAction.CategorySelected(category)
                    )
                    isCategorySheetVisible = false
                },
            )
        }
    }
}
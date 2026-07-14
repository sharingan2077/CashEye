package com.yandex.school.casheye.feature.transactions.editor.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AddTransactionRoute(
    type: TransactionType,
    onSaved: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(type) {
        viewModel.start(type)
    }

    AddTransactionContent(
        state = state,
        onAction = viewModel::onAction,
    )
}
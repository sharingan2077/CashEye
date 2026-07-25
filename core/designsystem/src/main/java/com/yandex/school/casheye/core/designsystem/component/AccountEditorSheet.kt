package com.yandex.school.casheye.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.core.designsystem.R
import kotlinx.coroutines.android.awaitFrame

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEditorSheet(
    name: String,
    balance: String,
    currency: String,
    isEditing: Boolean,
    isSaving: Boolean,
    error: String?,
    onNameChange: (String) -> Unit,
    onBalanceChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    var nested by remember { mutableStateOf<AccountNestedSheet?>(null) }
    var shouldRequestAmountFocus by remember { mutableStateOf(true) }
    var interceptedDismiss by remember { mutableStateOf(false) }
    val currentNested by rememberUpdatedState(nested)
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { target ->
                if (target == SheetValue.Hidden && currentNested != null) {
                    nested = null
                    interceptedDismiss = true
                    false
                } else {
                    true
                }
            },
        )
    LaunchedEffect(interceptedDismiss) {
        if (interceptedDismiss) {
            awaitFrame()
            interceptedDismiss = false
        }
    }

    EditorModalSheet(
        sheetState = sheetState,
        onDismiss = {
            if (interceptedDismiss) {
                interceptedDismiss = false
            } else if (nested != null) {
                nested = null
            } else {
                onDismiss()
            }
        },
    ) {
        when (nested) {
            AccountNestedSheet.Name ->
                EditorTextContent(
                    title = stringResource(R.string.finance_editor_account_name),
                    value = name,
                    placeholder = stringResource(R.string.finance_editor_account_name_placeholder),
                    singleLine = true,
                    onConfirm = {
                        onNameChange(it)
                        nested = null
                    },
                    onDismiss = { nested = null },
                )

            AccountNestedSheet.Currency ->
                CurrencyContent(
                    selectedCurrency = currency,
                    onSelect = {
                        onCurrencyChange(it)
                        nested = null
                    },
                )

            null ->
                FinanceEditorContent(
                    title =
                        stringResource(
                            if (isEditing) {
                                R.string.finance_editor_balance_adjustment
                            } else {
                                R.string.finance_editor_add_account
                            },
                        ),
                    amount = balance,
                    currency = currencySymbol(currency),
                    isSaving = isSaving,
                    isSaveEnabled = name.isNotBlank() && balance.isNotBlank(),
                    error = error,
                    onAmountChange = onBalanceChange,
                    onSave = onSave,
                    requestAmountFocus = shouldRequestAmountFocus,
                    onAmountFocusRequested = { shouldRequestAmountFocus = false },
                ) { clearPrimaryFocus ->
                    EditorRow(
                        icon = R.drawable.ic_editor_name,
                        label = stringResource(R.string.finance_editor_name),
                        value = name.ifBlank { stringResource(R.string.finance_editor_not_specified) },
                        onClick = {
                            clearPrimaryFocus()
                            nested = AccountNestedSheet.Name
                        },
                    )
                    EditorRow(
                        icon = R.drawable.ic_editor_currency,
                        label = stringResource(R.string.finance_editor_currency),
                        value = currencyShortLabel(currency),
                        onClick = {
                            clearPrimaryFocus()
                            nested = AccountNestedSheet.Currency
                        },
                        showDivider = false,
                    )
                }
        }
    }
}

@Composable
internal fun EditorAccountContent(
    accounts: List<EditorOption>,
    selectedId: Int?,
    onSelect: (EditorOption) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        EditorSheetTitle(stringResource(R.string.finance_editor_accounts))
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
            itemsIndexed(accounts, key = { _, account -> account.id }) { index, account ->
                EditorSelectionRow(
                    emoji = account.emoji,
                    title = account.label,
                    subtitle = null,
                    selected = account.id == selectedId,
                    isLast = index == accounts.lastIndex,
                    onClick = { onSelect(account) },
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun CurrencyContent(
    selectedCurrency: String,
    onSelect: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        EditorSheetTitle(stringResource(R.string.finance_editor_currency))
        currencies.forEachIndexed { index, currency ->
            EditorSelectionRow(
                emoji = currency.flag,
                title = stringResource(currency.titleRes),
                subtitle = currency.code,
                selected = currency.code == selectedCurrency,
                isLast = index == currencies.lastIndex,
                onClick = { onSelect(currency.code) },
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

private enum class AccountNestedSheet { Name, Currency }

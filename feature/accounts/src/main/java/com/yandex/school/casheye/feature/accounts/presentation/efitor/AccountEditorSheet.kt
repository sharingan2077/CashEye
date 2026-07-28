package com.yandex.school.casheye.feature.accounts.presentation.efitor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yandex.school.casheye.core.designsystem.R
import com.yandex.school.casheye.core.designsystem.component.editor.CurrencySelectionContent
import com.yandex.school.casheye.core.designsystem.component.editor.EditorModalSheet
import com.yandex.school.casheye.core.designsystem.component.editor.EditorRow
import com.yandex.school.casheye.core.designsystem.component.editor.EditorSheetTitle
import com.yandex.school.casheye.core.designsystem.component.editor.EditorTextContent
import com.yandex.school.casheye.core.designsystem.component.editor.FinanceEditorContent
import com.yandex.school.casheye.core.designsystem.component.money.currencySymbol
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEditorSheet(
    name: String,
    balance: String,
    currency: String,
    emoji: String,
    isEditing: Boolean,
    isSaving: Boolean,
    error: String?,
    onNameChange: (String) -> Unit,
    onBalanceChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    currencySelectionRequest: Int,
    onRequestCurrency: () -> Unit,
    onEmojiChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    var nested by remember { mutableStateOf<AccountNestedSheet?>(null) }
    var shouldRequestAmountFocus by remember { mutableStateOf(true) }
    val currentNested by rememberUpdatedState(nested)
    LaunchedEffect(currencySelectionRequest) {
        if (currencySelectionRequest > 0) nested = AccountNestedSheet.Currency
    }
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { target ->
                if (target == SheetValue.Hidden && currentNested != null) {
                    nested = null
                    false
                } else {
                    true
                }
            },
        )
    EditorModalSheet(
        sheetState = sheetState,
        onDismiss = {
            if (nested != null) {
                nested = null
            } else {
                onDismiss()
            }
        },
    ) {
        BackHandler(enabled = nested != null) { nested = null }
        when (nested) {
            AccountNestedSheet.Name -> {
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
            }

            AccountNestedSheet.Emoji -> {
                EmojiContent(
                    selectedEmoji = emoji,
                    onSelect = {
                        onEmojiChange(it)
                        nested = null
                    },
                )
            }

            AccountNestedSheet.Currency -> {
                CurrencyContent(
                    selectedCurrency = currency,
                    onSelect = {
                        onCurrencyChange(it)
                        nested = null
                    },
                )
            }

            null -> {
                FinanceEditorContent(
                    title =
                        stringResource(
                            if (isEditing) {
                                R.string.finance_editor_balance_adjustment
                            } else {
                                R.string.finance_editor_add_account
                            },
                        ),
                    titleStyle =
                        if (isEditing) {
                            MaterialTheme.typography.titleSmall
                        } else {
                            MaterialTheme.typography.titleMedium
                        },
                    titleColor =
                        if (isEditing) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            Color.Unspecified
                        },
                    amount = balance,
                    currency = currencySymbol(currency),
                    isSaving = isSaving,
                    isSaveEnabled = name.isNotBlank() && balance.isNotBlank(),
                    error = error,
                    onAmountChange = onBalanceChange,
                    onSave = onSave,
                    requestAmountFocus = shouldRequestAmountFocus,
                    onAmountFocusRequest = { shouldRequestAmountFocus = false },
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
                        icon = R.drawable.ic_editor_wallet,
                        label = stringResource(R.string.finance_editor_account_icon),
                        value = emoji,
                        onClick = {
                            clearPrimaryFocus()
                            nested = AccountNestedSheet.Emoji
                        },
                    )
                    EditorRow(
                        icon = R.drawable.ic_editor_currency,
                        label = stringResource(R.string.finance_editor_currency),
                        value = currencyShortLabel(currency),
                        onClick = {
                            clearPrimaryFocus()
                            if (isEditing) onRequestCurrency() else nested = AccountNestedSheet.Currency
                        },
                        showDivider = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmojiContent(
    selectedEmoji: String,
    onSelect: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        EditorSheetTitle(stringResource(R.string.finance_editor_account_icon))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth().height(248.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(accountEmojis, key = { it }) { emoji ->
                EmojiOption(
                    emoji = emoji,
                    selected = emoji == selectedEmoji,
                    onClick = { onSelect(emoji) },
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun EmojiOption(
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderWidth = if (selected) 2.dp else 1.dp
    val borderColor =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline
        }
    Box(
        modifier = Modifier.fillMaxWidth().height(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(borderWidth, borderColor, CircleShape)
                    .selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = onClick,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 28.sp)
        }
    }
}

@Composable
private fun CurrencyContent(
    selectedCurrency: String,
    onSelect: (String) -> Unit,
) =
    CurrencySelectionContent(
        title = stringResource(R.string.finance_editor_currency),
        selectedCurrency = selectedCurrency,
        onSelect = onSelect,
    )

private val accountEmojis =
    listOf(
        "💵",
        "💳",
        "💰",
        "🏦",
        "👛",
        "🪙",
        "💎",
        "📈",
        "🏠",
        "🚗",
        "✈️",
        "🎓",
        "🎁",
        "🐷",
        "🧾",
        "💼",
    )

@Composable
private fun currencyShortLabel(currencyCode: String): String {
    val titleRes =
        when (currencyCode.uppercase(Locale.ROOT)) {
            "RUB" -> R.string.finance_editor_currency_rub_short
            "USD" -> R.string.finance_editor_currency_usd_short
            "EUR" -> R.string.finance_editor_currency_eur_short
            "GBP" -> R.string.finance_editor_currency_gbp_short
            "CNY" -> R.string.finance_editor_currency_cny_short
            else -> return currencyCode.uppercase(Locale.ROOT)
        }
    return stringResource(titleRes)
}

private enum class AccountNestedSheet { Name, Emoji, Currency }

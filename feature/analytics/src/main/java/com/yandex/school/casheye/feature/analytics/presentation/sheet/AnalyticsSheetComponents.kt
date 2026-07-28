package com.yandex.school.casheye.feature.analytics.presentation.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.core.designsystem.component.editor.rememberSheetListGestureCoordinator
import com.yandex.school.casheye.feature.analytics.presentation.AnalyticsPeriod
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnalyticsModalBottomSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        content = content,
        dragHandle = {
            Box(
                modifier = Modifier.fillMaxWidth().height(24.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Spacer(
                    modifier =
                        Modifier
                            .padding(top = 12.dp)
                            .size(width = 32.dp, height = 4.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(100.dp)),
                )
            }
        },
    )
}

@Composable
internal fun SheetTitle(
    title: String,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    isDetails: Boolean = false,
) {
    Text(
        text = title,
        style =
            if (isDetails) {
                MaterialTheme.typography.headlineSmall
            } else {
                MaterialTheme.typography.headlineMedium
            },
        modifier = modifier.padding(paddingValues),
    )
}

@Composable
internal fun SheetButton(
    title: String,
    paddingValues: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    height: Dp = 48.dp,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .height(height),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
internal fun AnalyticsPeriod.formatted(): String {
    val locale = LocalConfiguration.current.locales[0] ?: LocalLocale.current.platformLocale
    val formatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale) }
    return "${startDate.format(formatter)} – ${endDate.format(formatter)}"
}

package com.yandex.school.casheye.feature.analytics.presentation.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.pie.PieChart
import com.patrykandpatrick.vico.compose.pie.PieChartHost
import com.patrykandpatrick.vico.compose.pie.PieSize
import com.patrykandpatrick.vico.compose.pie.data.PieChartModelProducer
import com.patrykandpatrick.vico.compose.pie.data.pieSeries
import com.patrykandpatrick.vico.compose.pie.rememberPieChart
import com.yandex.school.casheye.feature.analytics.R

@Composable
internal fun AnalyticsPieChart(
    total: String,
    items: List<AnalyticsPieChartItem>,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(vertical = 32.dp),
    showLegend: Boolean = true,
    modelProducer: PieChartModelProducer? = null,
    animateIn: Boolean = true,
    onChartDispose: (() -> Unit)? = null,
) {
    val internalModelProducer = remember { PieChartModelProducer() }
    val chartModelProducer = modelProducer ?: internalModelProducer
    val colors = items.map(AnalyticsPieChartItem::color)
    val chartColors = if (items.size == 1) colors + Color.Transparent else colors
    if (modelProducer == null) {
        LaunchedEffect(items) {
            chartModelProducer.runTransaction {
                pieSeries {
                    series(
                        analyticsPieChartValues(items),
                    )
                }
            }
        }
    }
    Column(
        modifier = modifier.fillMaxWidth().padding(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        PieChartWithTotal(total, chartColors, chartModelProducer, animateIn, onChartDispose)
        if (showLegend) AnalyticsLegend(items)
    }
}

@Composable
private fun PieChartWithTotal(
    total: String,
    colors: List<Color>,
    modelProducer: PieChartModelProducer,
    animateIn: Boolean,
    onChartDispose: (() -> Unit)?,
) {
    DisposableEffect(onChartDispose) { onDispose { onChartDispose?.invoke() } }
    Box(modifier = Modifier.size(240.dp), contentAlignment = Alignment.Center) {
        PieChartHost(
            chart =
                rememberPieChart(
                    sliceProvider =
                        PieChart.SliceProvider.series(
                            colors.map {
                                PieChart.Slice(
                                    fill = Fill(it),
                                )
                            },
                        ),
                    innerSize = PieSize.Inner.fixed(192.dp),
                ),
            modelProducer = modelProducer,
            modifier = Modifier.size(240.dp),
            animateIn = animateIn,
        )
        BoxWithConstraints(
            modifier = Modifier.size(192.dp).padding(horizontal = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            val textMeasurer = rememberTextMeasurer()
            val totalBaseStyle = MaterialTheme.typography.headlineMedium
            val fittedTotalTextStyle =
                remember(total, constraints.maxWidth, totalBaseStyle, textMeasurer) {
                    totalTextStyle(
                        total,
                        constraints.maxWidth,
                        totalBaseStyle,
                        textMeasurer,
                    )
                }
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.total_for_period),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    total,
                    modifier = Modifier.fillMaxWidth(),
                    style = fittedTotalTextStyle,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun totalTextStyle(
    total: String,
    availableWidth: Int,
    baseStyle: TextStyle,
    textMeasurer: TextMeasurer,
): TextStyle {
    val largestFontSize = baseStyle.fontSize.value.toInt()
    return (MINIMUM_TOTAL_FONT_SIZE.value.toInt()..largestFontSize)
        .reversed()
        .firstOrNull { fontSize ->
            textMeasurer
                .measure(
                    AnnotatedString(total),
                    baseStyle.copy(fontSize = fontSize.sp),
                    maxLines = 1,
                    softWrap = false,
                ).size.width <= availableWidth
        }?.let { baseStyle.copy(fontSize = it.sp) } ?: baseStyle.copy(fontSize = MINIMUM_TOTAL_FONT_SIZE)
}

@Composable
private fun AnalyticsLegend(items: List<AnalyticsPieChartItem>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.size(12.dp).background(item.color, CircleShape))
                Text(
                    item.label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

private val MINIMUM_TOTAL_FONT_SIZE = 1.sp

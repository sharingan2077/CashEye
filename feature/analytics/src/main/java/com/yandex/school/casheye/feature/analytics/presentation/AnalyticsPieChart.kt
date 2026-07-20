package com.yandex.school.casheye.feature.analytics.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.pie.PieChart
import com.patrykandpatrick.vico.compose.pie.PieChartHost
import com.patrykandpatrick.vico.compose.pie.PieSize
import com.patrykandpatrick.vico.compose.pie.data.PieChartModelProducer
import com.patrykandpatrick.vico.compose.pie.data.pieSeries
import com.patrykandpatrick.vico.compose.pie.rememberPieChart
import com.yandex.school.casheye.core.designsystem.theme.ChartPink
import com.yandex.school.casheye.core.designsystem.theme.ChartPurple
import com.yandex.school.casheye.core.designsystem.theme.ChartTeal

private val chartPalette = listOf(ChartPurple, ChartTeal, ChartPink)

@Composable
fun AnalyticsPieChart(
    total: String,
    categories: List<AnalyticsCategorySummary>,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(vertical = 32.dp),
    showLegend: Boolean = true,
) {
    val modelProducer = remember { PieChartModelProducer() }
    val colors = categories.map { analyticsColorForCategory(it.category.id) }
    LaunchedEffect(categories) {
        modelProducer.runTransaction { pieSeries { series(categories.map { it.amount }) } }
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        PieChartWithTotal(total = total, colors = colors, modelProducer = modelProducer)
        if (showLegend) AnalyticsLegend(categories = categories, colors = colors)
    }
}

@Composable
private fun PieChartWithTotal(
    total: String,
    colors: List<Color>,
    modelProducer: PieChartModelProducer,
) {
    Box(modifier = Modifier.size(240.dp), contentAlignment = Alignment.Center) {
        PieChartHost(
            chart =
                rememberPieChart(
                    sliceProvider =
                        PieChart.SliceProvider.series(
                            colors.map { color -> PieChart.Slice(fill = Fill(color)) },
                        ),
                    innerSize = PieSize.Inner.fixed(192.dp),
                ),
            modelProducer = modelProducer,
            modifier = Modifier.size(240.dp),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Всего за период",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(text = total, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun AnalyticsLegend(
    categories: List<AnalyticsCategorySummary>,
    colors: List<Color>,
) {
    FlowRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEachIndexed { index, summary ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(
                    modifier =
                        Modifier
                            .size(12.dp)
                            .background(colors[index], CircleShape),
                )
                Text(
                    text = summary.category.name,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

internal fun analyticsColorForCategory(categoryId: Int): Color {
    val hash = categoryId * GOLDEN_RATIO_HASH
    val bucket = (hash.toLong() and UINT_MASK).rem(COLOR_BUCKETS).toInt()
    if (bucket < chartPalette.size) return chartPalette[bucket]

    val hue = (hash.toLong() and UINT_MASK).rem(FULL_HUE).toFloat()
    return Color.hsv(hue = hue, saturation = 0.58f, value = 0.88f)
}

private const val GOLDEN_RATIO_HASH = -1640531527
private const val UINT_MASK = 0xffffffffL
private const val COLOR_BUCKETS = 12L
private const val FULL_HUE = 360L

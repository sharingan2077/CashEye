package com.yandex.school.casheye.feature.analytics.presentaion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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

private val chartColors = listOf(ChartPurple, ChartTeal, ChartPink)

@Composable
fun AnalyticsPieChart(
    total: String,
    articles: Map<String, Int>,
    modifier: Modifier = Modifier,
) {
    val modelProducer = remember { PieChartModelProducer() }
    LaunchedEffect(articles) {
        modelProducer.runTransaction { pieSeries { series(articles.values) } }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        PieChartWithTotal(total = total, modelProducer = modelProducer)
        AnalyticsLegend(articles = articles)
    }
}

@Composable
private fun PieChartWithTotal(
    total: String,
    modelProducer: PieChartModelProducer,
) {
    Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
        PieChartHost(
            chart =
                rememberPieChart(
                    sliceProvider =
                        PieChart.SliceProvider.series(
                            chartColors.map { color -> PieChart.Slice(fill = Fill(color)) },
                        ),
                    innerSize = PieSize.Inner.fixed(172.dp),
                ),
            modelProducer = modelProducer,
            modifier = Modifier.size(220.dp),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Всего за период",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = total,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Composable
private fun AnalyticsLegend(articles: Map<String, Int>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        articles.keys.forEachIndexed { index, article ->
            LegendItem(title = article, color = chartColors[index % chartColors.size])
        }
    }
}

@Composable
private fun LegendItem(
    title: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier
            .size(12.dp)
            .background(color, CircleShape))
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

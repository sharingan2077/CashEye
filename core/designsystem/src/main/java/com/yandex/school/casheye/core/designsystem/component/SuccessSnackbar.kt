package com.yandex.school.casheye.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private data class SuccessSnackbarVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = true,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
) : SnackbarVisuals

suspend fun SnackbarHostState.showSuccessSnackbar(message: String) {
    showSnackbar(SuccessSnackbarVisuals(message))
}

@Composable
fun CashEyeSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        if (data.visuals is SuccessSnackbarVisuals) {
            SuccessSnackbar(data)
        } else {
            Snackbar(snackbarData = data)
        }
    }
}

@Composable
private fun SuccessSnackbar(data: SnackbarData) {
    Surface(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        color = SUCCESS_SNACKBAR_BACKGROUND,
        contentColor = Color.White,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .defaultMinSize(minHeight = 72.dp)
                    .padding(start = 20.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SuccessCheck(modifier = Modifier.size(40.dp))
            Text(
                text = data.visuals.message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            IconButton(onClick = data::dismiss) {
                DismissIcon(modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun SuccessCheck(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawCircle(
            color = SUCCESS_GREEN,
            style = Stroke(width = 3.dp.toPx()),
        )
        drawLine(
            color = SUCCESS_GREEN,
            start = Offset(size.width * 0.27f, size.height * 0.52f),
            end = Offset(size.width * 0.44f, size.height * 0.68f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = SUCCESS_GREEN,
            start = Offset(size.width * 0.44f, size.height * 0.68f),
            end = Offset(size.width * 0.75f, size.height * 0.35f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun DismissIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawLine(
            color = DISMISS_GRAY,
            start = Offset(size.width * 0.25f, size.height * 0.25f),
            end = Offset(size.width * 0.75f, size.height * 0.75f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = DISMISS_GRAY,
            start = Offset(size.width * 0.75f, size.height * 0.25f),
            end = Offset(size.width * 0.25f, size.height * 0.75f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private val SUCCESS_SNACKBAR_BACKGROUND = Color(0xFF242424)
private val SUCCESS_GREEN = Color(0xFF34A853)
private val DISMISS_GRAY = Color(0xFF9E9E9E)

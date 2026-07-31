package com.yandex.school.casheye.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.core.designsystem.R
import kotlin.math.roundToInt

@Composable
fun SwipeToRevealDeleteItem(
    actionLabel: String,
    isRevealed: Boolean,
    onReveal: () -> Unit,
    onDismissReveal: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val actionWidth = 88.dp
    val density = LocalDensity.current
    var actionWidthPx by remember { mutableFloatStateOf(with(density) { actionWidth.toPx() }) }
    var targetOffset by remember {
        mutableFloatStateOf(
            if (isRevealed) {
                -actionWidthPx
            } else {
                0f
            },
        )
    }
    var isDragging by remember { mutableStateOf(false) }
    var revealRequestedDuringDrag by remember { mutableStateOf(false) }
    val offset by
        animateFloatAsState(
            targetValue = targetOffset,
            animationSpec = if (isDragging) snap() else spring(),
            label = "deleteRevealOffset",
        )

    LaunchedEffect(isRevealed) {
        if (!isRevealed) {
            targetOffset = 0f
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clipToBounds(),
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize(),
        ) {
            Column(
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(actionWidth)
                        .background(MaterialTheme.colorScheme.error)
                        .onSizeChanged { actionWidthPx = it.width.toFloat() }
                        .clickable(onClick = onDelete),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete_outline),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onError,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = actionLabel,
                    color = MaterialTheme.colorScheme.onError,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .offset { IntOffset(offset.roundToInt(), 0) }
                    .background(MaterialTheme.colorScheme.surface)
                    .draggable(
                        state =
                            rememberDraggableState { delta ->
                                val previousOffset = targetOffset
                                targetOffset =
                                    (targetOffset + delta).coerceIn(-actionWidthPx, 0f)
                                val revealThreshold = -actionWidthPx / 2f
                                if (
                                    !revealRequestedDuringDrag &&
                                    previousOffset > revealThreshold &&
                                    targetOffset <= revealThreshold
                                ) {
                                    revealRequestedDuringDrag = true
                                    onReveal()
                                }
                            },
                        orientation = Orientation.Horizontal,
                        onDragStarted = {
                            isDragging = true
                            revealRequestedDuringDrag = targetOffset <= -actionWidthPx / 2f
                        },
                        onDragStopped = {
                            isDragging = false
                            targetOffset =
                                if (targetOffset <= -actionWidthPx / 2f) {
                                    -actionWidthPx
                                } else {
                                    onDismissReveal()
                                    0f
                                }
                        },
                    ).clickable {
                        if (targetOffset < 0f || isRevealed) {
                            targetOffset = 0f
                            onDismissReveal()
                        } else {
                            onClick()
                        }
                    },
        ) {
            content()
        }
    }
}

package com.yandex.school.casheye.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.core.designsystem.R
import kotlinx.coroutines.launch

@Composable
fun ScrollToTopButton(
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val isVisible by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }
    val coroutineScope = rememberCoroutineScope()

    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
    ) {
        FloatingActionButton(
            onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
            modifier = Modifier.size(56.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_scroll_to_top),
                contentDescription = stringResource(R.string.scroll_to_top),
            )
        }
    }
}

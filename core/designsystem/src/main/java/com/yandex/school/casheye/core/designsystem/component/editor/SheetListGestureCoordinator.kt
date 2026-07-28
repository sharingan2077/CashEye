package com.yandex.school.casheye.core.designsystem.component.editor

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

class SheetListGestureCoordinator(
    private val listState: LazyListState,
    private val listFlingBehavior: FlingBehavior,
) : NestedScrollConnection,
    FlingBehavior {
    private var userGestureInProgress = false
    private var blockSheetForGesture = true

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (
            source == NestedScrollSource.UserInput &&
            !userGestureInProgress &&
            available.y != 0f
        ) {
            userGestureInProgress = true
            blockSheetForGesture = listState.canScrollBackward || available.y < 0f
        }
        return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset =
        if (source != NestedScrollSource.UserInput || blockSheetForGesture) {
            Offset(x = 0f, y = available.y)
        } else {
            Offset.Zero
        }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity,
    ): Velocity {
        val consumedVelocity =
            if (userGestureInProgress && !blockSheetForGesture) {
                Velocity.Zero
            } else {
                Velocity(x = 0f, y = available.y)
            }
        userGestureInProgress = false
        blockSheetForGesture = true
        return consumedVelocity
    }

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        if (userGestureInProgress && !blockSheetForGesture) return initialVelocity

        val scrollScope = this
        return with(listFlingBehavior) {
            scrollScope.performFling(initialVelocity)
        }
    }
}

@Composable
fun rememberSheetListGestureCoordinator(listState: LazyListState): SheetListGestureCoordinator {
    val listFlingBehavior = ScrollableDefaults.flingBehavior()
    return remember(listState, listFlingBehavior) {
        SheetListGestureCoordinator(
            listState = listState,
            listFlingBehavior = listFlingBehavior,
        )
    }
}

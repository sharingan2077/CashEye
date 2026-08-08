package com.yandex.school.casheye.core.designsystem.component.editor

import androidx.compose.foundation.ScrollState
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

/**
 * Lets a list consume its own gestures while allowing a downward drag at its top to dismiss the
 * parent sheet.
 */
class SheetListGestureCoordinator(
    private val listState: LazyListState,
    private val listFlingBehavior: FlingBehavior,
) : SheetGestureCoordinator(),
    FlingBehavior {
    override fun canScrollBackward(): Boolean = listState.canScrollBackward

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        if (userGestureInProgress && !blockSheetForGesture) return initialVelocity

        val scrollScope = this
        return with(listFlingBehavior) {
            scrollScope.performFling(initialVelocity)
        }
    }
}

private class SheetTextFieldGestureCoordinator(
    private val scrollState: ScrollState,
) : SheetGestureCoordinator() {
    override fun canScrollBackward(): Boolean = scrollState.canScrollBackward
}

abstract class SheetGestureCoordinator : NestedScrollConnection {
    protected var userGestureInProgress = false
    protected var blockSheetForGesture = true

    protected abstract fun canScrollBackward(): Boolean

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
            blockSheetForGesture = canScrollBackward() || available.y < 0f
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
}

/** Remembers one coordinator and the matching default fling behavior for a sheet list. */
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

/** Remembers a coordinator that prevents a scrolling text field from dragging its parent sheet. */
@Composable
fun rememberSheetTextFieldGestureCoordinator(scrollState: ScrollState): NestedScrollConnection =
    remember(scrollState) { SheetTextFieldGestureCoordinator(scrollState) }

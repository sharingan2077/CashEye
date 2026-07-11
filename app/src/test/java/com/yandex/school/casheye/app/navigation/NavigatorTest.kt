package com.yandex.school.casheye.app.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigatorTest {

    @Test
    fun `navigate switches to a top-level destination`() {
        val state = navigationState()

        Navigator(state).navigate(Route.Income)

        assertEquals(Route.Income, state.topLevelRoute)
        assertEquals(listOf(Route.Expenses, Route.Income), state.stackInUse)
    }

    @Test
    fun `navigate pushes a nested route onto the active stack`() {
        val state = navigationState()
        val navigator = Navigator(state)
        navigator.navigate(Route.Income)

        navigator.navigate(TransactionDetails)

        assertEquals(
            listOf(Route.Income, TransactionDetails),
            state.backStacks.getValue(Route.Income),
        )
    }

    @Test
    fun `goBack removes a nested route before leaving its top-level destination`() {
        val state = navigationState()
        val navigator = Navigator(state)
        navigator.navigate(Route.Income)
        navigator.navigate(TransactionDetails)

        navigator.goBack()

        assertEquals(Route.Income, state.topLevelRoute)
        assertEquals(listOf(Route.Income), state.backStacks.getValue(Route.Income))
    }

    @Test
    fun `goBack from a top-level destination returns to the start destination`() {
        val state = navigationState()
        val navigator = Navigator(state)
        navigator.navigate(Route.Account)

        navigator.goBack()

        assertEquals(Route.Expenses, state.topLevelRoute)
    }

    private fun navigationState(): NavigationState {
        val startRoute = Route.Expenses
        val routes = listOf(Route.Expenses, Route.Income, Route.Account)

        return NavigationState(
            startRoute = startRoute,
            topLevelRoute = mutableStateOf(startRoute),
            backStacks = routes.associateWith { route -> NavBackStack(route) },
        )
    }

    private data object TransactionDetails : NavKey
}

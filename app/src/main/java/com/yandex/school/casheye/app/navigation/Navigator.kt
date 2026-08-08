package com.yandex.school.casheye.app.navigation

import androidx.navigation3.runtime.NavKey

/** Applies navigation actions to the selected top-level route and its independent back stack. */
class Navigator(
    val state: NavigationState,
) {
    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            state.topLevelRoute = route
        } else {
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    fun goBack() {
        val currentStack =
            state.backStacks[state.topLevelRoute]
                ?: error("Back stack for ${state.topLevelRoute} doesn't exist")
        val currentRoute = currentStack.last()

        if (currentRoute == state.topLevelRoute) {
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }
}

package com.yandex.school.casheye.app.navigation.chrome

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.R
import com.yandex.school.casheye.app.navigation.NavigationContent
import com.yandex.school.casheye.app.navigation.NavigationState
import com.yandex.school.casheye.app.navigation.Navigator
import com.yandex.school.casheye.app.navigation.NetworkRecoveryRefresh
import com.yandex.school.casheye.app.navigation.Route
import com.yandex.school.casheye.app.navigation.toAnalyticsEntryPoint
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NavigationScaffold(
    navigationState: NavigationState,
    snackbarHostState: SnackbarHostState,
    navigator: Navigator,
    selectedDate: LocalDate,
    networkRecoveryRefresh: NetworkRecoveryRefresh?,
    onNetworkRecoveryRefresh: (Long) -> Unit,
    onEditExpense: (Int) -> Unit,
    onEditIncome: (Int) -> Unit,
    onEditAccount: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onDateClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val currentRoute = navigationState.backStacks[navigationState.topLevelRoute]?.lastOrNull()
    val showMainChrome = currentRoute in TOP_LEVEL_DESTINATIONS

    Scaffold(
        modifier = modifier,
        bottomBar = {
            AnimatedVisibility(
                visible = showMainChrome,
                enter =
                    slideInVertically(
                        animationSpec = tween(APP_CHROME_ANIMATION_DURATION_MILLIS),
                        initialOffsetY = { it },
                    ) +
                        expandVertically(
                            animationSpec = tween(APP_CHROME_ANIMATION_DURATION_MILLIS),
                            expandFrom = Alignment.Bottom,
                        ),
                exit =
                    slideOutVertically(
                        animationSpec = tween(APP_CHROME_ANIMATION_DURATION_MILLIS),
                        targetOffsetY = { it },
                    ) +
                        shrinkVertically(
                            animationSpec = tween(APP_CHROME_ANIMATION_DURATION_MILLIS),
                            shrinkTowards = Alignment.Bottom,
                        ),
                label = "bottom navigation visibility",
            ) {
                BottomNavigationBar(
                    selectedKey = navigationState.topLevelRoute,
                    onSelectKey = navigator::navigate,
                )
            }
        },
        topBar = {
            AnimatedContent(
                targetState = showMainChrome,
                transitionSpec = {
                    fadeIn(tween(APP_CHROME_ANIMATION_DURATION_MILLIS)) togetherWith
                        fadeOut(tween(APP_CHROME_ANIMATION_DURATION_MILLIS))
                },
                label = "top bar transition",
            ) { showMain ->
                if (showMain) {
                    NavigationTopBar(
                        date = selectedDate,
                        onDateClick = onDateClick,
                        onSettingsClick = onSettingsClick,
                        onAnalyticsClick = {
                            navigator.navigate(
                                Route.Analytics(navigationState.topLevelRoute.toAnalyticsEntryPoint()),
                            )
                        },
                    )
                } else {
                    ArrowTopBar(title = stringResource(R.string.analytics_title), onBackClick = navigator::goBack)
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showMainChrome,
                enter =
                    fadeIn(tween(APP_CHROME_ANIMATION_DURATION_MILLIS)) +
                        scaleIn(
                            animationSpec = tween(APP_CHROME_ANIMATION_DURATION_MILLIS),
                            initialScale = FAB_INITIAL_SCALE,
                        ),
                exit =
                    fadeOut(tween(APP_CHROME_ANIMATION_DURATION_MILLIS)) +
                        scaleOut(
                            animationSpec = tween(APP_CHROME_ANIMATION_DURATION_MILLIS),
                            targetScale = FAB_INITIAL_SCALE,
                        ),
                label = "floating action button visibility",
            ) {
                FloatingButton(onClick = onAddClick)
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { innerPadding ->
        NavigationContent(
            navigationState = navigationState,
            snackbarHostState = snackbarHostState,
            navigator = navigator,
            selectedDate = selectedDate,
            networkRecoveryRefresh = networkRecoveryRefresh,
            onNetworkRecoveryRefresh = onNetworkRecoveryRefresh,
            onEditExpense = onEditExpense,
            onEditIncome = onEditIncome,
            onEditAccount = onEditAccount,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun FloatingButton(onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick, modifier = Modifier.size(56.dp)) {
        Icon(
            painter = painterResource(R.drawable.plus),
            contentDescription = stringResource(R.string.content_description_add),
            modifier = Modifier.size(24.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun FloatingButtonPreview() {
    CashEyeTheme(dynamicColor = false) { FloatingButton {} }
}

internal const val APP_CHROME_ANIMATION_DURATION_MILLIS = 220
private const val FAB_INITIAL_SCALE = 0.8f

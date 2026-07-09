package com.yandex.school.casheye.feature.income

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


@Composable
fun IncomeScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = Modifier,
        topBar = {
            IncomeTopBar()
        },
        bottomBar = {
            IncomeBottomBar()
        },
        floatingActionButton = {
            IncomeFloatingActionButton()
        }
    ) { innerPadding ->

        Column(modifier = Modifier.padding(innerPadding)

        ) {




        }

    }

}

@Composable
fun IncomeTopBar() {



}

@Composable
fun IncomeBottomBar() {

}

@Composable
fun IncomeFloatingActionButton() {

}

@Composable
fun IncomeHero() {
    Column() {

        Text(
            text = "доходы, всего",
            style = MaterialTheme.typography.titleSmall,
            )
    }
}

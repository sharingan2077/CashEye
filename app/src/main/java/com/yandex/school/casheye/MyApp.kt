package com.yandex.school.casheye

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.yandex.school.casheye.core.navigation.NavigationRoot
import com.yandex.school.casheye.feature.expense.ExpenseScreen
import com.yandex.school.casheye.feature.income.IncomeScreen
import kotlinx.serialization.Serializable


@Composable
fun MyApp() {

    MaterialTheme {
        NavigationRoot()
    }


}
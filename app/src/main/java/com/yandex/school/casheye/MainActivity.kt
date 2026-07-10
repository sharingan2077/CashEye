package com.yandex.school.casheye

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.yandex.school.casheye.feature.expense.ExpenseScreen
import com.yandex.school.casheye.ui.theme.CashEyeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CashEyeTheme {

                MyApp()


//                ExpenseScreen(modifier = Modifier.fillMaxSize())
            }


        }
    }
}

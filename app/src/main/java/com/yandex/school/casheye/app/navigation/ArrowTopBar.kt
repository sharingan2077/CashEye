package com.yandex.school.casheye.app.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArrowTopBar(
    title: String,
    onBackClick: () -> Unit
) {

    TopAppBar(
        title = {
            Text(
                text = title,
            )
        },
        navigationIcon = {

            IconButton(
                modifier = Modifier
                    .size(48.dp),
                onClick = onBackClick
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = "Назад",

                    )
            }
        }
    )
}
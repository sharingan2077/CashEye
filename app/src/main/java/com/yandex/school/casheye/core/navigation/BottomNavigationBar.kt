package com.yandex.school.casheye.core.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.yandex.school.casheye.ui.theme.CashEyeTheme


@Composable
fun BottomNavigationBar(
    selectedKey: NavKey,
    onSelectKey: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = MaterialTheme.colorScheme.outline

    NavigationBar(
        modifier = modifier.drawWithContent {
            drawContent()

            val strokeWidth = 1.dp.toPx()
            drawLine(
                color = borderColor,
                start = Offset(0f, strokeWidth / 2),
                end = Offset(size.width, strokeWidth / 2),
                strokeWidth = strokeWidth,
            )
        },
    ) {

        TOP_LEVEL_DESTINATIONS.forEach { (destination, data) ->
            NavigationBarItem(
                selected = selectedKey == destination,
                onClick = { onSelectKey(destination) },
                icon = {
                    Icon(
                        painter = painterResource(data.iconRes),
                        contentDescription = data.title,
                    )
                },
                label = { Text(text = data.title) },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun BottomNavigationBarPreview() {
    CashEyeTheme(dynamicColor = false) {
        BottomNavigationBar(
            selectedKey = Route.Expenses,
            onSelectKey = {},
        )
    }
}

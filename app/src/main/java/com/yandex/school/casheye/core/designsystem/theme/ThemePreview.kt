package com.yandex.school.casheye.core.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun ThemePreview() {
    CashEyeTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ColorPreviewRow()
                TypographyPreviewBlock()
                Button(onClick = {}) {
                    Text(text = "Primary action")
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = "Surface variant card",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorPreviewRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ColorSwatch(MaterialTheme.colorScheme.primary)
        ColorSwatch(MaterialTheme.colorScheme.background)
        ColorSwatch(MaterialTheme.colorScheme.surfaceVariant)
        ColorSwatch(MaterialTheme.colorScheme.outline)
        ColorSwatch(MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun ColorSwatch(color: Color) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(color = color, shape = RoundedCornerShape(8.dp)),
    )
}

@Composable
private fun TypographyPreviewBlock() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "1 322 444 RUB",
            style = MaterialTheme.typography.displayMedium,
        )
        Text(
            text = "Section title",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "List item title",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Secondary label",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

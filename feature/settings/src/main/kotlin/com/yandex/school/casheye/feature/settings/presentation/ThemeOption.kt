package com.yandex.school.casheye.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.domain.settings.ThemeMode

@Composable
internal fun ThemeOption(
    label: String,
    painter: Painter,
    mode: ThemeMode,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color(0xFFE0E0E0)
    val optionShape = RoundedCornerShape(16.dp)
    val previewShape = RoundedCornerShape(8.dp)
    Column(
        modifier =
            modifier
                .border(2.dp, borderColor, optionShape)
                .clip(optionShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(role = Role.RadioButton, onClick = onClick)
                .testTag("settings_theme_${mode.name.lowercase()}")
                .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .border(1.dp, Color(0xFFE0E0E0), previewShape)
                    .clip(previewShape)
                    .background(
                        when (mode) {
                            ThemeMode.LIGHT -> Brush.verticalGradient(listOf(Color.White, Color.White))
                            ThemeMode.DARK -> Brush.verticalGradient(listOf(Color(0xFF1C1B1F), Color(0xFF1C1B1F)))
                            ThemeMode.SYSTEM -> Brush.verticalGradient(listOf(Color.White, Color.Black))
                        },
                    ),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(painter, null, Modifier.size(16.dp), MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

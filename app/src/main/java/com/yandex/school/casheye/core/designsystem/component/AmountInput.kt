package com.yandex.school.casheye.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textStyle = MaterialTheme.typography.displayMedium.copy(
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )

    Column(
        modifier = modifier.width(200.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = { input ->
                onValueChange(
                    input
                        .filter(Char::isDigit)
                        .take(12)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = textStyle,
            cursorBrush = SolidColor(
                Color(0xFFB69DF8),
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
            ),
            visualTransformation = RubleSuffixVisualTransformation,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = "0 ₽",
                            style = textStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    innerTextField()
                }
            },
        )

        HorizontalDivider(
            modifier = Modifier.width(200.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}


private object RubleSuffixVisualTransformation : VisualTransformation {

    private const val Suffix = " ₽"

    override fun filter(text: AnnotatedString): TransformedText {
        if (text.isEmpty()) {
            return TransformedText(
                text = text,
                offsetMapping = OffsetMapping.Identity,
            )
        }

        val originalLength = text.length
        val transformedText = AnnotatedString(
            text = text.text + Suffix,
        )

        val offsetMapping = object : OffsetMapping {

            override fun originalToTransformed(offset: Int): Int {
                return if (offset == originalLength) {
                    offset + Suffix.length
                } else {
                    offset
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                return offset.coerceAtMost(originalLength)
            }
        }

        return TransformedText(
            text = transformedText,
            offsetMapping = offsetMapping,
        )
    }
}

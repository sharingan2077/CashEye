package com.yandex.school.casheye.feature.settings.presentation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.feature.settings.R

@Composable
fun AppLockScreen(
    biometricsEnabled: Boolean,
    onPinSubmit: (CharArray) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPin by remember { mutableStateOf(!biometricsEnabled) }
    var pin by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(showPin) {
        if (showPin) focusRequester.requestFocus()
    }

    Box(
        modifier = modifier.fillMaxSize().padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text =
                    androidx.compose.ui.res
                        .stringResource(R.string.app_lock_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text =
                    androidx.compose.ui.res.stringResource(
                        if (showPin) R.string.app_lock_pin_hint else R.string.app_lock_biometrics_hint,
                    ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (showPin) {
                Box(contentAlignment = Alignment.Center) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(4) { index ->
                            Box(
                                modifier =
                                    Modifier
                                        .size(48.dp)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(if (index < pin.length) "•" else "")
                            }
                        }
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = pin,
                        onValueChange = { input ->
                            val digits = input.filter(Char::isDigit).take(4)
                            pin = digits
                            if (digits.length == 4) {
                                onPinSubmit(digits.toCharArray())
                                pin = ""
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .alpha(0f)
                                .focusRequester(focusRequester)
                                .testTag("app_lock_pin_input"),
                    )
                }
            } else {
                androidx.compose.material3.TextButton(onClick = { showPin = true }) {
                    Text(
                        androidx.compose.ui.res
                            .stringResource(R.string.app_lock_use_pin),
                    )
                }
            }
        }
    }
}

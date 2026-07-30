package com.yandex.school.casheye.feature.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.feature.settings.R

private val PIN_KEY_HEIGHT = 60.dp
private val PIN_KEY_VISUAL_SIZE = 60.dp
private val PIN_ACTION_ICON_SIZE = 32.dp

@Composable
internal fun AppLockNumberPad(
    showFingerprint: Boolean,
    showBackspace: Boolean,
    enabled: Boolean,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onFingerprint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        listOf("123", "456", "789").forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { digit ->
                    PinKey(
                        label = digit.toString(),
                        onClick = { onDigit(digit) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f).height(PIN_KEY_HEIGHT))
            PinKey(
                label = "0",
                onClick = { onDigit('0') },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            when {
                showFingerprint -> {
                    PinActionKey(
                        painterRes = R.drawable.ic_settings_fingerprint,
                        description = stringResource(R.string.app_lock_retry_biometrics),
                        testTag = "app_lock_fingerprint",
                        onClick = onFingerprint,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    )
                }

                showBackspace -> {
                    PinActionKey(
                        painterRes = R.drawable.ic_settings_back,
                        description = stringResource(R.string.app_lock_backspace),
                        testTag = "app_lock_backspace",
                        onClick = onBackspace,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    )
                }

                else -> {
                    Box(modifier = Modifier.weight(1f).height(PIN_KEY_HEIGHT))
                }
            }
        }
    }
}

@Composable
private fun PinKey(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier =
            modifier
                .height(PIN_KEY_HEIGHT)
                .semantics { contentDescription = label }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ).testTag("app_lock_key_$label"),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(PIN_KEY_VISUAL_SIZE)
                    .clip(CircleShape)
                    .indication(interactionSource, ripple()),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Normal),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun PinActionKey(
    painterRes: Int,
    description: String,
    testTag: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier =
            modifier
                .height(PIN_KEY_HEIGHT)
                .semantics { contentDescription = description }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ).testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(PIN_KEY_VISUAL_SIZE)
                    .clip(CircleShape)
                    .indication(interactionSource, ripple()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(painterRes),
                contentDescription = null,
                modifier = Modifier.size(PIN_ACTION_ICON_SIZE),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

package com.yandex.school.casheye.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.core.designsystem.R
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme

@Composable
fun ErrorState(
    type: ErrorStateType,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    retryLabel: String? = null,
) {
    val content = type.content()
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(content.imageRes),
            contentDescription = stringResource(content.titleRes),
            modifier = Modifier.size(200.dp),
        )
        Text(
            text = stringResource(content.titleRes),
            modifier = Modifier.padding(top = 16.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(content.descriptionRes),
            modifier = Modifier.widthIn(max = 280.dp).padding(top = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null && retryLabel != null) {
            Button(
                onClick = onRetry,
                modifier = Modifier.padding(top = 24.dp),
            ) {
                Text(text = retryLabel)
            }
        }
    }
}

private data class ErrorStateContent(
    val imageRes: Int,
    val titleRes: Int,
    val descriptionRes: Int,
)

private fun ErrorStateType.content(): ErrorStateContent =
    when (this) {
        ErrorStateType.Network -> {
            ErrorStateContent(
                imageRes = R.drawable.image_error_network,
                titleRes = R.string.error_network_title,
                descriptionRes = R.string.error_network_description,
            )
        }

        ErrorStateType.Server -> {
            ErrorStateContent(
                imageRes = R.drawable.image_error_server,
                titleRes = R.string.error_server_title,
                descriptionRes = R.string.error_server_description,
            )
        }

        ErrorStateType.Authorization -> {
            ErrorStateContent(
                imageRes = R.drawable.image_error_authorization,
                titleRes = R.string.error_authorization_title,
                descriptionRes = R.string.error_authorization_description,
            )
        }

        ErrorStateType.Unknown -> {
            ErrorStateContent(
                imageRes = R.drawable.image_error_loading,
                titleRes = R.string.error_unknown_title,
                descriptionRes = R.string.error_unknown_description,
            )
        }
    }

@Preview(name = "Error state light", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Error state dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ErrorStatePreview(
    @PreviewParameter(ErrorStateTypeProvider::class) type: ErrorStateType,
) {
    CashEyeTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ErrorState(type = type)
        }
    }
}

enum class ErrorStateType {
    Network,
    Server,
    Authorization,
    Unknown,
}

private class ErrorStateTypeProvider : PreviewParameterProvider<ErrorStateType> {
    override val values = ErrorStateType.entries.asSequence()
}

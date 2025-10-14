package com.firstapp.paypaldemo.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


@Composable
fun BrowserSwitchLauncher(
    isLoading: Boolean,
    message: String,
    showRetryButton: Boolean,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement =
            Arrangement.spacedBy(10.dp, alignment = Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val progressAlpha = if (isLoading) 1.0f else 0.0f
        val retryButtonAlpha = if (showRetryButton && !isLoading) 1.0f else 0.0f
        CircularProgressIndicator(modifier = Modifier.alpha(progressAlpha))
        Text(
            text = message,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = onRetry,
            modifier = Modifier
                .alpha(retryButtonAlpha)
                .defaultMinSize(minHeight = 48.dp)
        ) {
            Text(
                text = "Confirm", modifier = Modifier
                    .padding(horizontal = 32.dp)
            )
        }
    }
}

@Preview
@Composable
fun BrowserSwitchLauncherPreviewInitial() {
    BrowserSwitchLauncher(
        isLoading = true,
        message = "Sample Message",
        showRetryButton = false,
        onRetry = {}
    )
}

@Preview
@Composable
fun BrowserSwitchLauncherPreviewAllowRetry() {
    BrowserSwitchLauncher(
        isLoading = false,
        message = "Sample Message",
        showRetryButton = true,
        onRetry = {}
    )
}

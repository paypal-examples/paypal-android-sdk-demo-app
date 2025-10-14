package com.firstapp.paypaldemo.paymentlink

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.firstapp.paypaldemo.main.CheckoutState
import com.paypal.android.utils.OnLifecycleOwnerResumeEffect
import com.paypal.android.utils.OnNewIntentEffect
import com.paypal.android.utils.getActivityOrNull

private val PAYMENT_LINK_URI = "https://www.sandbox.paypal.com/ncp/payment/BFXRZ54VKCAQ6".toUri()

@Composable
fun PayWithPaymentLink(
    onOrderComplete: (orderId: String) -> Unit,
    viewModel: PayWithPaymentLinkViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Capture LocalContext reference to obtain a ComponentActivity reference
    // when BrowserSwitch launch is requested
    val context = LocalContext.current

    // When PayWithPayPal is presented, immediately launch BrowserSwitch flow
    LaunchedEffect(Unit) {
        context.getActivityOrNull()?.let { activity ->
            viewModel.launchUri(activity, PAYMENT_LINK_URI)
        }
    }

    // Handle finishing BrowserSwitch return from Chrome Custom Tab
    OnNewIntentEffect { newIntent ->
        viewModel.finishPayWithPaymentLink(newIntent)
    }

    // Also attempt to finish BrowserSwitch from cold start after a process kill
    OnLifecycleOwnerResumeEffect {
        val intent = context.getActivityOrNull()?.intent
        intent?.let { viewModel.finishPayWithPaymentLink(it) }
    }

    // Notify Order Complete
    LaunchedEffect(uiState.checkoutState) {
        (uiState.checkoutState as? CheckoutState.OrderComplete)?.let { result ->
            onOrderComplete(result.orderId)
        }
    }

    val isLoading = when (uiState.checkoutState) {
        is CheckoutState.OrderCreateInProgress, is CheckoutState.StartPayPalInProgress -> true
        else -> false
    }
    BrowserSwitchLauncher(
        isLoading = isLoading,
        showRetryButton = uiState.didInitiateCheckout,
        onRetry = {
            context.getActivityOrNull()?.let { activity ->
                viewModel.launchUri(activity, PAYMENT_LINK_URI)
            }
        }
    )
}

@Composable
fun BrowserSwitchLauncher(
    isLoading: Boolean,
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
        val message = if (isLoading) {
            "Redirecting to Pay with Payment Link"
        } else {
            "Confirm your Payment"
        }

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
            Text(text = "Confirm", modifier = Modifier
                .padding(horizontal = 32.dp)
            )
        }
    }
}

@Preview
@Composable
fun BrowserSwitchLauncherPreviewInitial() {
    BrowserSwitchLauncher(isLoading = true, showRetryButton = false, onRetry = {})
}

@Preview
@Composable
fun BrowserSwitchLauncherPreviewAllowRetry() {
    BrowserSwitchLauncher(isLoading = false, showRetryButton = true, onRetry = {})
}

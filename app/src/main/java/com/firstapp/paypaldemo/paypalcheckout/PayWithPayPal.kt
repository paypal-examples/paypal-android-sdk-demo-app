package com.firstapp.paypaldemo.paypalcheckout

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.firstapp.paypaldemo.main.CheckoutState
import com.paypal.android.utils.OnLifecycleOwnerResumeEffect
import com.paypal.android.utils.OnNewIntentEffect
import com.paypal.android.utils.getActivityOrNull

@Composable
fun PayWithPayPal(
    onOrderComplete: (orderId: String) -> Unit,
    viewModel: PayPalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Capture LocalContext reference to obtain a ComponentActivity reference
    // when PayPal launch is requested
    val context = LocalContext.current

    // When PayWithPayPal is presented, immediately launch PayPal flow
    LaunchedEffect(Unit) {
        context.getActivityOrNull()?.let { activity ->
            viewModel.startPayPalCheckout(activity = activity)
        }
    }

    // Handle finishing PayPal return from Chrome Custom Tab
    OnNewIntentEffect { newIntent ->
        viewModel.finishPayPalCheckout(newIntent)
    }

    // Also attempt to finish PayPal from cold start after a process kill
    OnLifecycleOwnerResumeEffect {
        val intent = context.getActivityOrNull()?.intent
        intent?.let { viewModel.finishPayPalCheckout(it) }
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
    PayWithPayPal(
        isLoading = isLoading,
        showRetryButton = uiState.didInitiateCheckout,
        onRetryStartPayPal = {
            context.getActivityOrNull()?.let { activity ->
                viewModel.startPayPalCheckout(activity = activity)
            }
        }
    )
}

@Composable
fun PayWithPayPal(
    isLoading: Boolean,
    showRetryButton: Boolean,
    onRetryStartPayPal: () -> Unit
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
            "Redirecting to PayPal"
        } else {
            "Confirm your PayPal Account"
        }

        CircularProgressIndicator(modifier = Modifier.alpha(progressAlpha))
        Text(
            text = message,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = onRetryStartPayPal,
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
fun PayWithPayPalPreviewInitial() {
    PayWithPayPal(isLoading = true, showRetryButton = false, onRetryStartPayPal = {})
}

@Preview
@Composable
fun PayWithPayPalPreviewAllowRetry() {
    PayWithPayPal(isLoading = false, showRetryButton = true, onRetryStartPayPal = {})
}

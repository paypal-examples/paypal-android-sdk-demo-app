package com.firstapp.paypaldemo.paymentlink

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.firstapp.paypaldemo.main.CheckoutState
import com.firstapp.paypaldemo.ui.shared.BrowserSwitchLauncher
import com.firstapp.paypaldemo.utils.OnLifecycleOwnerResumeEffect
import com.firstapp.paypaldemo.utils.OnNewIntentEffect
import com.firstapp.paypaldemo.utils.getActivityOrNull

private val PAYMENT_LINK_URI = "https://www.sandbox.paypal.com/ncp/payment/BFXRZ54VKCAQ6".toUri()

@Composable
fun PayWithPaymentLink(
    onOrderComplete: () -> Unit,
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
        viewModel.finishPayWithPaymentLink(intent)
    }

    // Notify Order Complete
    LaunchedEffect(uiState.checkoutState) {
        (uiState.checkoutState as? CheckoutState.PaymentLinkComplete)?.let { result ->
            onOrderComplete()
        }
    }

    val isLoading = when (uiState.checkoutState) {
        is CheckoutState.OrderCreateInProgress, is CheckoutState.StartPayPalInProgress -> true
        else -> false
    }

    val message = if (isLoading) "Redirecting to Pay with Payment Link" else "Confirm your Payment"
    BrowserSwitchLauncher(
        isLoading = isLoading,
        message = message,
        showRetryButton = uiState.didInitiateCheckout,
        onRetry = {
            context.getActivityOrNull()?.let { activity ->
                viewModel.launchUri(activity, PAYMENT_LINK_URI)
            }
        }
    )
}


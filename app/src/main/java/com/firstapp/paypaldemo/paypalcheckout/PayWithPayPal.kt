package com.firstapp.paypaldemo.paypalcheckout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.firstapp.paypaldemo.main.CheckoutState
import com.firstapp.paypaldemo.ui.shared.BrowserSwitchLauncher
import com.firstapp.paypaldemo.utils.OnLifecycleOwnerResumeEffect
import com.firstapp.paypaldemo.utils.OnNewIntentEffect
import com.firstapp.paypaldemo.utils.getActivityOrNull

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
        viewModel.finishPayPalCheckout(intent)
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
    val message = if (isLoading) "Redirecting to PayPal" else "Confirm your PayPal Account"
    BrowserSwitchLauncher(
        isLoading = isLoading,
        message = message,
        showRetryButton = uiState.didInitiateCheckout,
        onRetry = {
            context.getActivityOrNull()?.let { activity ->
                viewModel.startPayPalCheckout(activity = activity)
            }
        }
    )
}

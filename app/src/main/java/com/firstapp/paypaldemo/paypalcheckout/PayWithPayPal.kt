package com.firstapp.paypaldemo.paypalcheckout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.firstapp.paypaldemo.main.CheckoutState
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

    // Notify Order Complete
    LaunchedEffect(uiState.checkoutState) {
        (uiState.checkoutState as? CheckoutState.OrderComplete)?.let { result ->
            onOrderComplete(result.orderId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text("Redirecting to PayPal...")
    }
}

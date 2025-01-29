package com.firstapp.paypaldemo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import com.firstapp.paypaldemo.ui.theme.PayPalDemoTheme

class MainActivity : ComponentActivity() {

    // Instead of a PayPalViewModel, we have a single coordinator
    private val coordinatorViewModel: CheckoutCoordinatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Let the coordinator create/hold the PayPalWebCheckoutClient.
        coordinatorViewModel.setActivityForPayPalClient(this)

        setContent {
            PayPalDemoTheme {
                // Observe the coordinator's state to see if there's an error or order complete
                val checkoutState = coordinatorViewModel.checkoutState.collectAsState()

                // Basic “router” approach or wrap in your NavHost:
                CheckoutFlow(
                    onPayWithPayPal = { amount ->
                        // Hardcode an orderId for testing
                        val orderId = "9UW08484WT2900113"
                        coordinatorViewModel.startPayPalCheckout(this, orderId)
                    },
                    onPayWithCard = { amount ->
                        // Card flow, or just navigate to "order complete" for now
                        // ...
                    },
                    checkoutState = checkoutState.value,
                    onDismissError = {
                        coordinatorViewModel.resetState()
                    },
                    onDismissComplete = {
                        coordinatorViewModel.resetState()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Must call setIntent so that calling getIntent() gets the latest
        setIntent(intent)
        // Let the coordinator handle finishing PayPal after browser return
        coordinatorViewModel.handleOnNewIntent(this, intent)
    }
}

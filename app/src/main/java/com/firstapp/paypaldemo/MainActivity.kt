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

        setContent {
            PayPalDemoTheme {
                // Observe the coordinator's state to see if there's an error or order complete
                val checkoutState = coordinatorViewModel.checkoutState.collectAsState()

                // Basic “router” approach or wrap in your NavHost:
                CheckoutFlow(
                    onPayWithPayPal = { amount ->
                        // Hardcode an orderId for testing
                        coordinatorViewModel.setActivityForPayPalClient(this.applicationContext)
                        coordinatorViewModel.startPayPalCheckout(this, amount)
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
        // Let the coordinator handle finishing PayPal after browser return
        coordinatorViewModel.handleOnNewIntent(intent)
    }
}

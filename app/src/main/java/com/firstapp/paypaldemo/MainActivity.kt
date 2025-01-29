package com.firstapp.paypaldemo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient

// These are for testing purposes. New orderID needs to be generated for each run
const val CLIENT_ID =
    "AQ04yLjwYNK_cZvD-S-HZY1TwV22AygaJ0JSiYdyqTcfcwRL6i8thQxKdTCZROmUou86wza_xoDk1WGz"


class MainActivity : ComponentActivity() {
    private lateinit var payPalClient: PayPalWebCheckoutClient

    private val viewModel: PayPalViewModel by viewModels {
        PayPalViewModelFactory(payPalClient)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val coreConfig = CoreConfig(CLIENT_ID)
        payPalClient = PayPalWebCheckoutClient(this, coreConfig, "com.firstapp.paypaldemo")

        savedInstanceState?.getString("authState")?.let { restoredAuthState ->
            viewModel.setAuthState(restoredAuthState)
        }

        setContent {
            CheckoutFlow(activity = this, viewModel = viewModel)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.getAuthState()?.let { authState ->
            outState.putString("authState", authState)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        viewModel.finishPayPalCheckout(
            activity = this,
            intent = intent
        )
    }
}
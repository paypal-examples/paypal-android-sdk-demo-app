package com.firstapp.paypaldemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import com.firstapp.paypaldemo.main.CheckoutCoordinatorViewModel
import com.firstapp.paypaldemo.main.CheckoutFlow
import com.firstapp.paypaldemo.ui.theme.PayPalDemoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val coordinatorViewModel: CheckoutCoordinatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PayPalDemoTheme {
                // Observe the coordinator's state to see if there's an error or order complete
                val checkoutState = coordinatorViewModel.checkoutState.collectAsState()

                // Basic “router” approach or wrap in your NavHost:
                CheckoutFlow(
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
}
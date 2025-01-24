package com.firstapp.paypaldemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.firstapp.paypaldemo.ui.theme.PayPalDemoTheme
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.paypalwebpayments.PayPalPresentAuthChallengeResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishStartResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFundingSource
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutRequest
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController

// These are for testing purposes. New orderID needs to be generated for each run
const val CLIENT_ID =
    "AQ04yLjwYNK_cZvD-S-HZY1TwV22AygaJ0JSiYdyqTcfcwRL6i8thQxKdTCZROmUou86wza_xoDk1WGz"
const val ORDER_ID = "4KK67643RP547063E"

class MainActivity : ComponentActivity() {
    private var payPalClient: PayPalWebCheckoutClient? = null
    private var authState: String? = null
    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val coreConfig = CoreConfig(CLIENT_ID)
        payPalClient = PayPalWebCheckoutClient(this, coreConfig, "com.firstapp.paypaldemo")

        setContent {
            PayPalDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()
                    this.navController = navController
                    val checkoutError = remember { mutableStateOf<String?>(null)}
                    NavHost(
                       navController = navController,
                        startDestination = "cart",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("cart") {
                            CartView(
                                onPayWithPayPal = { amount ->
                                    startPayPalCheckout(
                                        // replace with new orderID for each run. This is place holder for this phase
                                        orderId = ORDER_ID,
                                        onSuccess = { orderId ->
                                                navController.navigate("orderComplete/$orderId")
                                        },
                                        onError = { error ->
                                            checkoutError.value = error
                                        }
                                    )
                                    println("Pay with PayPal: $amount")

                                },
                                onPayWithCard = { amount ->
                                    println("Pay with Card: $amount")
                                    navController.navigate("orderComplete")
                                }
                            )
                        }
                        composable("orderComplete/{orderId}") { backStackEntry ->
                            OrderCompleteView(
                                orderID = backStackEntry.arguments?.getString("orderId") ?: "",
                                onDone = {
                                    println("Done!")
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    } // onCreate

    private fun startPayPalCheckout(
        orderId: String, onSuccess: (String) -> Unit, onError: (String) -> Unit
    ) {
        val request = PayPalWebCheckoutRequest(
            orderId = orderId,
            fundingSource = PayPalWebCheckoutFundingSource.PAYPAL
        )

        when (val result = payPalClient?.start(this, request)) {
            is PayPalPresentAuthChallengeResult.Success -> {
                authState = result.authState }
            is PayPalPresentAuthChallengeResult.Failure -> { onError(result.error.toString()) }
            null -> {
                onError("Failed to initialize PayPal client")
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePayPalResult(intent)
    }


    override fun onSaveInstanceState(outState: Bundle ) {
        super.onSaveInstanceState(outState)
       authState?.let { outState.putString("authState", it)}
    }

    private fun handlePayPalResult(intent: Intent) {
        val result = authState?.let { state -> payPalClient?.finishStart(intent, state) }
        when (result) {
            is PayPalWebCheckoutFinishStartResult.Success -> {
                navController?.navigate("orderComplete/${result.orderId}")
            }

            is PayPalWebCheckoutFinishStartResult.Canceled -> {
                println("Canceled")
            }

            is PayPalWebCheckoutFinishStartResult.Failure -> {
                println("WebCheckout failure")
            }

            PayPalWebCheckoutFinishStartResult.NoResult, null -> {
                println("No result was returned")
            }
        }
    }
}


@Composable
fun OnLifecycleOwnerResumeEffect(callback: () -> Unit) {
    // Ref: https://stackoverflow.com/a/66549433
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            callback()
        }
    }
}
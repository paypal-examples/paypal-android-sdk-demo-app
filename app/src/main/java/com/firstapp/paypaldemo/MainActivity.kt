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
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishStartResult

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PayPalDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "cart",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("cart") {
                            CartView(
                                onPayPalPaymentSuccess = {
                                    navController.navigate("orderComplete")
                                },
                                onPayWithCard = { amount ->
                                    println("Pay with Card: $amount")
                                    navController.navigate("orderComplete")
                                }
                            )
                        }
                        composable("orderComplete") {
                            OrderCompleteView(
                                orderID = "testID",
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
    }
}

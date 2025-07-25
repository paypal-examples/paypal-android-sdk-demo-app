package com.firstapp.paypaldemo.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.firstapp.paypaldemo.R
import com.firstapp.paypaldemo.cardcheckout.CardCheckoutView
import com.firstapp.paypaldemo.paypalcheckout.PayWithPayPal

// NOTE: The shopping cart in this example is static. This code snippet should draw a parallel
// to the data layer in your own application
private val shoppingCartItems =
    listOf(Item(name = "White T-shirt", amount = 29.99, imageResId = R.drawable.tshirt))

@ExperimentalMaterial3Api
@Composable
fun CheckoutFlow(
    onPayWithLink: (Double) -> Unit,
    checkoutState: CheckoutState,
    onDismissError: () -> Unit,
    onDismissComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val coordinator: CheckoutCoordinatorViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    NavHost(navController = navController, startDestination = "cart", modifier = modifier) {
        composable("cart") {
            CartView(
                onPayWithLink = onPayWithLink,
                shoppingCartItems = shoppingCartItems,
                onPayWithCard = { amount -> navController.navigate("cardCheckout/$amount") },
                onPayWithPayPal = {
                    navController.navigate("payPalCheckout") {
                        popUpTo("cart")
                    }
                },
            )
        }

        composable("cardCheckout/{amount}") { backStackEntry ->
            val previousDestination = navController.previousBackStackEntry

            DisposableEffect(Unit) {
                onDispose {
                    if (previousDestination?.destination?.route == "cart") {
                        coordinator.resetState()
                    }
                }
            }
            val amountParam = backStackEntry.arguments?.getString("amount") ?: "0.0"
            val amountDouble = amountParam.toDoubleOrNull() ?: 0.0
            CardCheckoutView(
                amount = amountDouble,
                onOrderCompleted = { orderId ->
                    navController.navigate("orderComplete/$orderId")
                }
            )
        }

        composable("payPalCheckout") {
            PayWithPayPal(
                onOrderComplete = { orderId ->
                    navController.navigate("orderComplete/$orderId") {
                        popUpTo("cart")
                    }
                }
            )
        }

        composable("orderComplete/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: "Unknown"
            OrderCompleteView(orderID = orderId) {
                onDismissComplete()
                navController.popBackStack(route = "cart", inclusive = false)
            }
        }

        composable("paymentLinkComplete/{amount}") { backStackEntry ->
            val amount = backStackEntry.arguments?.getString("amount") ?: "Unknown"
            PaymentLinkCompleteView(amount = amount) {
                onDismissComplete()
                navController.popBackStack(route = "cart", inclusive = false)
            }
        }
    }

    // React to coordinator's state changes:
    when (checkoutState) {
        is CheckoutState.Loading -> {
            LoadingOverlay(checkoutState.message)
        }

        is CheckoutState.OrderComplete -> {
            // If the coordinator says we’re “complete”, navigate to orderComplete
            LaunchedEffect(checkoutState) {
                val orderId = checkoutState.orderId
                navController.navigate("orderComplete/$orderId")
            }
        }

        is CheckoutState.PaymentLinkComplete -> {
            // navigate to orderComplete for payment link
            LaunchedEffect(checkoutState) {
                checkoutState.uri.getQueryParameter("amt")?.let { amount ->
                    navController.navigate("paymentLinkComplete/${amount}")
                }
            }
        }

        is CheckoutState.Error -> {
            // Show an alert
            AlertDialog(
                onDismissRequest = { onDismissError() },
                title = { Text("Error") },
                text = { Text(checkoutState.message) },
                confirmButton = {
                    Button(onClick = { onDismissError() }) {
                        Text("OK")
                    }
                }
            )
        }

        else -> {} // Idle: do nothing
    }
}

@Composable
fun LoadingOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        // Column in the center for a spinner + message.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = Color.White)
            Spacer(modifier = Modifier.size(16.dp))
            Text(text = message, color = Color.White)
        }
    }
}
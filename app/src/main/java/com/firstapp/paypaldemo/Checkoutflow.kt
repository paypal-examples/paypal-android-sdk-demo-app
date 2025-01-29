package com.firstapp.paypaldemo

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun CheckoutFlow(
    onPayWithPayPal: (Double) -> Unit,
    onPayWithCard: (Double) -> Unit,
    checkoutState: CheckoutState,
    onDismissError: () -> Unit,
    onDismissComplete: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "cart") {
        composable("cart") {
            CartView(
                onPayWithPayPal = onPayWithPayPal,
                onPayWithCard = onPayWithCard
            )
        }

        composable("orderComplete/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: "Unknown"
            OrderCompleteView(orderID = orderId) {
                onDismissComplete()
                navController.popBackStack()
            }
        }
    }

    // React to coordinator's state changes:
    when (checkoutState) {
        is CheckoutState.OrderComplete -> {
            // If the coordinator says we’re “complete”, navigate to orderComplete
            LaunchedEffect(checkoutState) {
                val orderId = checkoutState.orderId
                navController.navigate("orderComplete/$orderId")
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
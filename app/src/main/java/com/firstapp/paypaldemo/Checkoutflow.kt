package com.firstapp.paypaldemo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

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
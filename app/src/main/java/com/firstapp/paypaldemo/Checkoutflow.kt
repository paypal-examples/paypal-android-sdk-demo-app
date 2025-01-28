package com.firstapp.paypaldemo

import androidx.activity.ComponentActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable


@Composable
fun CheckoutFlow(
    activity: ComponentActivity,
    viewModel: PayPalViewModel,
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = "cart"
    ) {
        composable("cart") {
            CartView(
                onPayWithPayPal = { amount ->
                    val orderId = "2TJ24279FS4927634" // Hardcoded for now
                    viewModel.startPayPalCheckout(activity, orderId) },
                onPayWithCard = { amount ->
                    navController.navigate("orderComplete/testOrderId")
                }
            )
        }

        composable("orderComplete/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: "Unknown"
            OrderCompleteView(
                orderID = orderId,
                onDone = {
                    navController.popBackStack()
                }
            )
        }
    }

    val orderIdState = viewModel.orderId.collectAsState()
    orderIdState.value?.let { orderId ->
        navController.navigate("orderComplete/$orderId")
        viewModel.clearOrderId()
    }

    val errorState = viewModel.errorMessage.collectAsState()
    errorState.value?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                Button(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}

package com.firstapp.paypaldemo.main

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.firstapp.paypaldemo.R
import com.firstapp.paypaldemo.cardcheckout.CardCheckoutView
import com.firstapp.paypaldemo.paymentlink.PayWithPaymentLink
import com.firstapp.paypaldemo.paypalcheckout.PayWithPayPal

// NOTE: The shopping cart in this example is static. This code snippet should draw a parallel
// to the data layer in your own application
val shoppingCartItems =
    listOf(Item(name = "10 Credit Points", amount = 19.99, imageResId = R.drawable.gold))

@ExperimentalMaterial3Api
@Composable
fun CheckoutFlow(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "cart", modifier = modifier) {
        composable("cart") {
            CartView(
                onPayWithLink = {
                    navController.navigate("paymentLink") { popUpTo("cart") }
                },
                shoppingCartItems = shoppingCartItems,
                onPayWithCard = { amount -> navController.navigate("cardCheckout/$amount") },
                onPayWithPayPal = {
                    navController.navigate("payPalCheckout") { popUpTo("cart") }
                },
            )
        }

        composable("cardCheckout/{amount}") { backStackEntry ->
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

        composable("paymentLink") { backStackEntry ->
            PayWithPaymentLink(
                onOrderComplete = { orderId ->
                    navController.navigate("orderComplete/$orderId") {
                        popUpTo("cart")
                    }
                }
            )
        }

        composable("orderComplete/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: "Unknown"
            OrderCompleteView(
                orderID = orderId,
                onDone = { navController.popBackStack(route = "cart", inclusive = false) }
            )
        }
    }
}

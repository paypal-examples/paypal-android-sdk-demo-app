package com.firstapp.paypaldemo.main

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.firstapp.paypaldemo.Constants.SHOPPING_CART_ITEMS
import com.firstapp.paypaldemo.cardcheckout.CardCheckoutView
import com.firstapp.paypaldemo.paymentlink.PayWithPaymentLink
import com.firstapp.paypaldemo.paypalcheckout.PayWithPayPal

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
                shoppingCartItems = SHOPPING_CART_ITEMS,
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
                    navController.navigate("orderComplete?orderId=$orderId")
                }
            )
        }

        composable("payPalCheckout") {
            PayWithPayPal(
                onOrderComplete = { orderId ->
                    navController.navigate("orderComplete?orderId=$orderId") {
                        popUpTo("cart")
                    }
                }
            )
        }

        composable("paymentLink") { backStackEntry ->
            PayWithPaymentLink(
                onOrderComplete = {
                    navController.navigate("orderComplete") {
                        popUpTo("cart")
                    }
                }
            )
        }

        composable(
            "orderComplete?orderId={orderId}",
            arguments = listOf(
                navArgument("orderId") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId")
            OrderCompleteView(
                orderId = orderId,
                onDone = { navController.popBackStack(route = "cart", inclusive = false) }
            )
        }
    }
}

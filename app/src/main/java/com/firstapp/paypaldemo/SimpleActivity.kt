package com.firstapp.paypaldemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.firstapp.paypaldemo.service.Order
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.paymentbuttons.PayPalButton
import com.paypal.android.paymentbuttons.PayPalButtonColor
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient

// CartItem.kt
data class CartItem(val name: String, val price: Double)

// API.kt
class API {

    suspend fun createOrder(): Result<Order> {
        // TODO: create
        return Result.failure(Throwable("could not create order."))
    }
}

// SimpleActivity.kt
class SimpleActivity : ComponentActivity() {

    val cartItems = listOf(
        CartItem(name = "Selkirk Pro Amped Air", price = 179.99),
        CartItem(name = "Joola Ben Johns", price = 229.99)
    )

    // TODO: change to Environment.LIVE when your app is ready for production
    val environment = Environment.SANDBOX
    val config = CoreConfig(clientId = "<YOUR_CLIENT_ID_HERE>", environment = environment)

    val urlScheme = "com.firstapp"
    val payPalClient =
        PayPalWebCheckoutClient(context = this, configuration = config, urlScheme = urlScheme)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CartView(modifier = Modifier.padding(innerPadding)) {
                        launchPayPal()
                    }
                }
            }
        }
    }

    private fun launchPayPal() {

    }
}

@Composable
fun CartView(modifier: Modifier = Modifier, onPayPalButtonClick: () -> Unit = {}) {
    Column(modifier = modifier) {
        PayPalButtonView(modifier = Modifier.fillMaxWidth(), onClick = onPayPalButtonClick)
    }
}

@Composable
fun PayPalButtonView(modifier: Modifier = Modifier, onClick: () -> Unit) {
    AndroidView(
        factory = { context ->
            PayPalButton(context).apply {
                setOnClickListener { onClick() }
            }
        },
        update = { button ->
            button.color = PayPalButtonColor.BLUE
        },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MaterialTheme {
        CartView()
    }
}
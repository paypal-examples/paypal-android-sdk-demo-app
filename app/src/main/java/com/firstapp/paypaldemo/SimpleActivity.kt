package com.firstapp.paypaldemo

import android.content.Intent
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
import androidx.lifecycle.lifecycleScope
import com.firstapp.paypaldemo.service.Order
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.paymentbuttons.PayPalButton
import com.paypal.android.paymentbuttons.PayPalButtonColor
import com.paypal.android.paypalwebpayments.PayPalPresentAuthChallengeResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishStartResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutRequest
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

// CartItem.kt
data class CartItem(val name: String, val price: Double)

// OrderRequest.kt
data class OrderRequest(val amount: Double)

// API.kt
interface API {

    @POST("/orders")
    suspend fun createOrder(@Body orderRequest: OrderRequest): Response<Order>

    @POST("/orders/{orderId}/capture")
    suspend fun captureOrder(@Path("orderId") orderId: String): Response<Order>
}

// SimpleActivity.kt
class SimpleActivity : ComponentActivity() {

    companion object {
        fun createAPI(baseUrl: String): API {
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .build()
            return retrofit.create(API::class.java)
        }
    }

    // TODO: change to Environment.LIVE when your app is ready for production
    val environment = Environment.SANDBOX
    val config = CoreConfig(clientId = "<YOUR_CLIENT_ID_HERE>", environment = environment)
    val urlScheme = "com.firstapp"
    val payPalClient =
        PayPalWebCheckoutClient(context = this, configuration = config, urlScheme = urlScheme)

    val api = createAPI("https://api.myserver.com")
    val cartItems = listOf(
        CartItem(name = "Selkirk Pro Amped Air", price = 179.99),
        CartItem(name = "Joola Ben Johns", price = 229.99)
    )

    var authState: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CartView(
                        modifier = Modifier.padding(innerPadding),
                        onPayPalButtonClick = { launchPayPal() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val pendingAuthState = authState
        if (intent != null && pendingAuthState != null) {
            lifecycleScope.launch {
                completePayPalAuthChallenge(intent, pendingAuthState)
            }
        }
    }

    private fun launchPayPal() {
        lifecycleScope.launch {
            val amount = cartItems.sumOf { it.price }
            val request = OrderRequest(amount = amount)
            val response = api.createOrder(request)
            if (response.isSuccessful) {
                val order = response.body()
                presentPayPalAuthChallenge(order = order!!)
            } else {
                TODO("handle order creation error")
            }
        }
    }

    private fun presentPayPalAuthChallenge(order: Order) {
        val request = PayPalWebCheckoutRequest(orderId = order.id)
        val payPalStartResult =
            payPalClient.start(activity = this@SimpleActivity, request = request)
        when (payPalStartResult) {
            is PayPalPresentAuthChallengeResult.Success -> {
                // capture auth state
                authState = payPalStartResult.authState
            }

            is PayPalPresentAuthChallengeResult.Failure -> TODO("handle paypal start error")
        }
    }

    private suspend fun completePayPalAuthChallenge(intent: Intent, state: String) {
        when (val checkoutResult = payPalClient.finishStart(intent, state)) {
            is PayPalWebCheckoutFinishStartResult.Success -> {
                // Capture or authorize order on your server
                api.captureOrder(checkoutResult.orderId!!)
                discardAuthState()
            }

            is PayPalWebCheckoutFinishStartResult.Failure -> {
                // TODO: Handle approve order failure
                discardAuthState()
            }

            is PayPalWebCheckoutFinishStartResult.Canceled -> {
                // TODO: Notify user PayPal checkout was canceled
                discardAuthState()
            }

            PayPalWebCheckoutFinishStartResult.NoResult -> {
                // There isn't enough information to determine the state of the auth
                // challenge for this payment method; do nothing to preserve pending
                // auth state in case the PayPal Auth flow is completed at a later time
            }
        }
    }

    private fun discardAuthState() {
        // prevent future finish events from being parsed with stale auth state
        authState = null
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
            PayPalButton(context).apply { setOnClickListener { onClick() } }
        },
        update = { button -> button.color = PayPalButtonColor.BLUE },
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
package com.firstapp.paypaldemo.docs.card

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.firstapp.paypaldemo.service.Order
import com.paypal.android.cardpayments.Card
import com.paypal.android.cardpayments.CardApproveOrderResult
import com.paypal.android.cardpayments.CardAuthChallenge
import com.paypal.android.cardpayments.CardClient
import com.paypal.android.cardpayments.CardFinishApproveOrderResult
import com.paypal.android.cardpayments.CardPresentAuthChallengeResult
import com.paypal.android.cardpayments.CardRequest
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

// CartItem.kt
data class OrderItem(val id: String, val name: String, val price: Double)

// OrderRequest.kt
data class OrderRequest(
    val items: List<OrderItem>,
    val currencyCode: String = "USD",
    val paymentIntent: String = "CAPTURE"
)

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
    val cardClient = CardClient(context = this, configuration = config)

    val api = createAPI("https://api.myserver.com")
    val items = listOf(
        OrderItem(id = "123", name = "Selkirk Pro Amped Air", price = 179.99),
        OrderItem(id = "456", name = "Joola Ben Johns", price = 229.99)
    )

    var authState: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var cardNumber by rememberSaveable { mutableStateOf("") }
            var cardExpirationMonth by rememberSaveable { mutableStateOf("") }
            var cardExpirationYear by rememberSaveable { mutableStateOf("") }
            var cardSecurityCode by rememberSaveable { mutableStateOf("") }

            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        TextField(
                            value = cardNumber,
                            onValueChange = { cardNumber = it },
                            label = { Text("Card Number") }
                        )
                        TextField(
                            value = cardExpirationMonth,
                            onValueChange = { cardExpirationMonth = it },
                            label = { Text("Expiration Month") }
                        )
                        TextField(
                            value = cardExpirationYear,
                            onValueChange = { cardExpirationYear = it },
                            label = { Text("Expiration Year") }
                        )
                        TextField(
                            value = cardSecurityCode,
                            onValueChange = { cardSecurityCode = it },
                            label = { Text("Security Code") }
                        )
                        Button(
                            onClick = {
                                val card = Card(
                                    number = cardNumber,
                                    expirationMonth = cardExpirationMonth,
                                    expirationYear = cardExpirationYear,
                                    securityCode = cardSecurityCode
                                )
                                createAndApproveOrder(card)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Checkout")
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val pendingAuthState = authState
        if (intent != null && pendingAuthState != null) {
            lifecycleScope.launch {
                completeApproveOrderAuthChallenge(intent, pendingAuthState)
            }
        }
    }

    private fun createAndApproveOrder(card: Card) {
        lifecycleScope.launch {
            val request = OrderRequest(items = items)
            val response = api.createOrder(request)
            if (response.isSuccessful) {
                val order = response.body()
                approveOrder(order = order!!, card = card)
            } else {
                TODO("handle order creation error")
            }
        }
    }

    private fun approveOrder(order: Order, card: Card) {
        val cardRequest = CardRequest(
            orderId = order.id,
            card = card,
            returnUrl = "com.firstapp://approve-order/return-url"
        )
        cardClient.approveOrder(cardRequest = cardRequest) { approveOrderResult ->
            when (approveOrderResult) {
                is CardApproveOrderResult.Success -> captureOrder(approveOrderResult.orderId)
                is CardApproveOrderResult.AuthorizationRequired ->
                    presentApproveOrderAuthChallenge(approveOrderResult.authChallenge)

                is CardApproveOrderResult.Failure -> TODO("handle approve order error")
            }
        }
    }

    private fun captureOrder(orderId: String) {
        lifecycleScope.launch {
            val response = api.captureOrder(orderId)
            if (response.isSuccessful) {
                TODO("show order capture success to user")
            } else {
                TODO("show order capture failure to user")
            }
        }
    }

    private fun presentApproveOrderAuthChallenge(authChallenge: CardAuthChallenge) {
        when (val presentAuthChallengeResult =
            cardClient.presentAuthChallenge(this, authChallenge)) {
            is CardPresentAuthChallengeResult.Success -> {
                // capture auth state
                authState = presentAuthChallengeResult.authState
            }

            is CardPresentAuthChallengeResult.Failure -> TODO("show present auth challenge failure to user")
        }
    }

    private suspend fun completeApproveOrderAuthChallenge(intent: Intent, state: String) {
        when (val authChallengeResult = cardClient.finishApproveOrder(intent, state)) {
            is CardFinishApproveOrderResult.Success -> {
                captureOrder(authChallengeResult.orderId)
                discardAuthState()
            }

            is CardFinishApproveOrderResult.Failure -> {
                // TODO: Handle approve order failure
                discardAuthState()
            }

            CardFinishApproveOrderResult.Canceled -> {
                // TODO: Notify user Card approve order was canceled
                discardAuthState()
            }

            CardFinishApproveOrderResult.NoResult -> {
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

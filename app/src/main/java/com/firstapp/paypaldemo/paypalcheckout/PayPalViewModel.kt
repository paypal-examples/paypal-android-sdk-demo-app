package com.firstapp.paypaldemo.paypalcheckout

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.firstapp.paypaldemo.R
import com.firstapp.paypaldemo.main.CLIENT_ID
import com.firstapp.paypaldemo.main.CartUiState
import com.firstapp.paypaldemo.main.CheckoutState
import com.firstapp.paypaldemo.main.Item
import com.firstapp.paypaldemo.service.Amount
import com.firstapp.paypaldemo.service.DemoMerchantAPI
import com.firstapp.paypaldemo.service.PurchaseUnit
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.paypalwebpayments.PayPalPresentAuthChallengeResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishStartResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFundingSource
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PayPalViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel() {

    private val coreConfig = CoreConfig(CLIENT_ID)
    private val payPalClient =
        PayPalWebCheckoutClient(context, coreConfig, "com.firstapp.paypaldemo")

    private var authState: String? = null

    private var _uiState = MutableStateFlow(defaultCartUiState)
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    private var checkoutState
        get() = _uiState.value.checkoutState
        set(value) = _uiState.update { prevState -> prevState.copy(checkoutState = value) }

    /**
     * Launches the PayPal web checkout flow via Braintree browser switch library
     * Braintree browser switch library is a wrapper for Chrome Custom Tab.
     * onSuccess means the user was successfully sent to the browser.
     * The final 'finish' must still happen in handleOnNewIntent => finishPayPalCheckout.
     */
    fun startPayPalCheckout(activity: ComponentActivity) {
        checkoutState = CheckoutState.Loading("Starting PayPal Checkout")
        viewModelScope.launch {
            try {
                val items = _uiState.value.items
                val purchaseUnits = items.map { item ->
                    PurchaseUnit(amount = Amount(currencyCode = "USD", item.amount.toString()))
                }
                val order =
                    DemoMerchantAPI.createOrder(intent = "CAPTURE", purchaseUnits = purchaseUnits)
                println("✅ Created order ${order.id}, status: ${order.status}")

                val fundingSource = PayPalWebCheckoutFundingSource.PAYPAL
                val request =
                    PayPalWebCheckoutRequest(orderId = order.id, fundingSource = fundingSource)
                when (val result = payPalClient.start(activity, request)) {
                    is PayPalPresentAuthChallengeResult.Success -> {
                        // Preserve authentication state until we are able to call finish i.e. the
                        // user has authorized their payment method and we are deep linked back into
                        // the application via onNewIntent
                        authState = result.authState
                    }

                    is PayPalPresentAuthChallengeResult.Failure ->
                        checkoutState = CheckoutState.Error(result.error.toString())
                }
            } catch (e: Exception) {
                val errorMessage = "❌ Failed to create order on merchant server: ${e.message}"
                checkoutState = CheckoutState.Error(errorMessage)
            }
        }
    } // startPayPalCheckout

    /**
     * Called after the user returns from the Chrome Custom Tab to finish the checkout.
     */
    fun finishPayPalCheckout(intent: Intent) = checkIfPayPalAuthFinished(intent)?.let { result ->
        when (result) {
            is PayPalWebCheckoutFinishStartResult.Success -> {
                val orderId = result.orderId
                if (orderId == null) {
                    checkoutState =
                        CheckoutState.Error("received success but PayPal returned a null orderId")
                } else {
                    completeOrder(orderId)
                }
                discardAuthState()
            }

            is PayPalWebCheckoutFinishStartResult.Failure -> {
                checkoutState = CheckoutState.Error(result.error.toString())
                discardAuthState()
            }

            is PayPalWebCheckoutFinishStartResult.Canceled -> {
                checkoutState = CheckoutState.Error("Checkout canceled by user.")
                discardAuthState()
            }

            is PayPalWebCheckoutFinishStartResult.NoResult -> {
                // Control has been passed to Chrome Custom Tab. By making the UI idle,
                // The user's intent cannot be determined by the SDK. By returning the UI
                // to an idle state, we can give users the opportunity to relaunch the flow
                // e.g. if they accidentally closed the Chrome Custom Tab and need to re-launch it
                checkoutState = CheckoutState.Idle
            }
        }
    }

    // Only check for PayPal Auth completion when auth state exists
    private fun checkIfPayPalAuthFinished(intent: Intent): PayPalWebCheckoutFinishStartResult? =
        authState?.let { payPalClient.finishStart(intent, it) }

    private fun completeOrder(orderId: String) {
        viewModelScope.launch {
            val finalOrder = DemoMerchantAPI.completeOrder(orderId, "CAPTURE")
            println("✅ captured order: ${finalOrder.id}")
            checkoutState = CheckoutState.OrderComplete(finalOrder.id)
        }
    }

    private fun discardAuthState() {
        // Always discard auth state when a transaction is considered finished
        // e.g. Success, Failure and Canceled states. You may choose to clear auth state when
        // there is NoResult, but you will in that case need to create a new order to launch
        // the PayPal Web flow
        authState = null
    }

    companion object {
        private val defaultCartUiState by lazy {
            val items =
                listOf(Item(name = "White T-shirt", amount = 29.99, imageResId = R.drawable.tshirt))
            val totalAmount = items.sumOf { it.amount }
            CartUiState(
                items = items,
                totalAmount = totalAmount,
                checkoutState = CheckoutState.Idle
            )
        }
    }
}

package com.firstapp.paypaldemo.paypalcheckout

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.firstapp.paypaldemo.main.CLIENT_ID
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
import kotlinx.coroutines.launch

@HiltViewModel
class PayPalViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel() {

    private var authState: String? = null

    val coreConfig = CoreConfig(CLIENT_ID)
    val payPalClient =
        PayPalWebCheckoutClient(context, coreConfig, "com.firstapp.paypaldemo")

    /**
     * Launches the PayPal web checkout flow via Braintree browser switch library
     * Braintree browser switch library is a wrapper for Chrome Custom Tab.
     * onSuccess means the user was successfully sent to the browser.
     * The final 'finish' must still happen in handleOnNewIntent => finishPayPalCheckout.
     */
    fun startPayPalCheckout(amount: Double, activity: ComponentActivity) {
        viewModelScope.launch {
            try {
                val order = DemoMerchantAPI.createOrder(
                    intent = "CAPTURE",
                    purchaseUnits = listOf(
                        PurchaseUnit(
                            amount = Amount(currencyCode = "USD", value = amount.toString())
                        )
                    )
                )
                println("✅ Created order ${order.id}, status: ${order.status}")

                val request = PayPalWebCheckoutRequest(
                    orderId = order.id,
                    fundingSource = PayPalWebCheckoutFundingSource.PAYPAL
                )

                when (val result = payPalClient.start(activity, request)) {
                    is PayPalPresentAuthChallengeResult.Success -> {
                        authState = result.authState
                        // TODO: update UIState
//                    onSuccess()
                    }

                    is PayPalPresentAuthChallengeResult.Failure -> {
                        // TODO: update UIState
//                    onFailure(result.error.toString())
                    }

                    else -> {
                        // TODO: update UIState
//                    onFailure("Unexpected error during checkout.")
                    }
                }
            } catch (e: Exception) {
                // TODO: update UIState
//            onFailure("❌ Failed to create order on merchant server: ${e.message}")
            }

        }
    } // startPayPalCheckout

    /**
     * Called after the user returns from the Chrome Custom Tab to finish the checkout.
     */
    fun finishPayPalCheckout(
        intent: android.content.Intent
//        onSuccess: (String) -> Unit,
//        onCanceled: () -> Unit,
//        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = authState?.let { existingAuthState ->
                payPalClient.finishStart(intent, existingAuthState)
            }

            when (result) {
                is PayPalWebCheckoutFinishStartResult.Success -> {
                    val orderId = result.orderId
                    if (orderId == null) {
                        // TODO: update UI
//                    onFailure("received success but PayPal returned a null orderId")
                    } else {
                        val finalOrder = DemoMerchantAPI.completeOrder(orderId, "CAPTURE")
                        println("✅ captured order: ${finalOrder.id}")

                        // TODO: update UI
//                    onSuccess(finalOrder.id)
                    }
                }

                is PayPalWebCheckoutFinishStartResult.Failure -> {
                    // TODO: update UI
//                onFailure(result.error.toString())
                }

                is PayPalWebCheckoutFinishStartResult.Canceled -> {
                    // TODO: update UI
//                onCanceled()
                }

                else -> {
                    // TODO: update UI
//                onFailure("Unexpected error occurred while completing the checkout.")
                }
            }

        }
    }
}


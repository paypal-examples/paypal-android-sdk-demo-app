package com.firstapp.paypaldemo

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalPresentAuthChallengeResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishStartResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFundingSource
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutRequest

class PayPalViewModel(
    private val payPalClient: PayPalWebCheckoutClient
) : ViewModel() {

    private var authState: String? = null

    /**
     * Launches the PayPal web checkout flow via Chrome Custom Tab.
     *
     * onSuccess means the user was successfully sent to the browser.
     * The final 'finish' must still happen in handleOnNewIntent => finishPayPalCheckout.
     */
    suspend fun startPayPalCheckout(
        activity: ComponentActivity,
        orderId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val request = PayPalWebCheckoutRequest(
            orderId = orderId,
            fundingSource = PayPalWebCheckoutFundingSource.PAYPAL
        )

        when (val result = payPalClient.start(activity, request)) {
            is PayPalPresentAuthChallengeResult.Success -> {
                authState = result.authState
                onSuccess()
            }
            is PayPalPresentAuthChallengeResult.Failure -> {
                onFailure(result.error.toString())
            }
            else -> {
                onFailure("Unexpected error during checkout.")
            }
        }
    }

    /**
     * Called after the user returns from the Chrome Custom Tab to finish the checkout.
     */
    suspend fun finishPayPalCheckout(
        intent: android.content.Intent,
        onSuccess: (String) -> Unit,
        onCanceled: () -> Unit,
        onFailure: (String) -> Unit
    ) {

        val result = authState?.let { existingAuthState ->
            payPalClient.finishStart(intent, existingAuthState)
        }

        when (result) {
            is PayPalWebCheckoutFinishStartResult.Success -> {
                val orderId = result.orderId
                if (orderId == null) {
                    onFailure("received success but PayPal returned a null orderId")
                } else {
                    onSuccess(orderId)
                }
            }
            is PayPalWebCheckoutFinishStartResult.Failure -> {
                onFailure(result.error.toString())
            }
            is PayPalWebCheckoutFinishStartResult.Canceled -> {
                onCanceled()
            }
            else -> {
                onFailure("Unexpected error occurred while completing the checkout.")
            }
        }
    }
}


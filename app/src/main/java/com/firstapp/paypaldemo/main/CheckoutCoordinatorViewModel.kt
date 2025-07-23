package com.firstapp.paypaldemo.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.firstapp.paypaldemo.paypalcheckout.PayPalViewModel
import com.firstapp.paypaldemo.service.DemoMerchantAPI
import com.paypal.android.cardpayments.CardClient
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

const val CLIENT_ID =
    "AQTfw2irFfemo-eWG4H5UY-b9auKihUpXQ2Engl4G1EsHJe2mkpfUv_SN3Mba0v3CfrL6Fk_ecwv9EOo"

/**
 * Simple sealed class representing states we might show:
 *  - Idle (Cart)
 *  - CardCheckout
 *  - Order Complete
 *  - Error
 *
 * You can expand this with “Loading” or other states as needed.
 */
sealed class CheckoutState {
    object Idle : CheckoutState()
    data class Loading(val message: String = "Loading...") : CheckoutState()
    data class OrderComplete(val orderId: String) : CheckoutState()
    data class Error(val message: String) : CheckoutState()
    data class PaymentLinkComplete(val uri: Uri): CheckoutState()
}

private const val TAG = "CheckoutCoordinatorViewModel"

/**
 * The coordinator ViewModel that orchestrates PayPal and Card payments.
 */
class CheckoutCoordinatorViewModel : ViewModel() {

    // The underlying PayPalWebCheckoutClient (depends on an Activity context).
    // We'll set it on PayPal button press from CartView
    private var payPalClient: PayPalWebCheckoutClient? = null

    // The specialized PayPalViewModel. We'll create it once we have a payPalClient.
    private var payPalViewModel: PayPalViewModel? = null

    // Live state (or “flow state”) to help the UI know what to display.
    private val _checkoutState = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
    val checkoutState: StateFlow<CheckoutState> = _checkoutState

    /**
     * Initialize or update the PayPal client any time we have a fresh Activity reference.
     * Alternatively, you can set up the client once in onCreate and pass it here.
     */
    fun initializePayPalClient(context: Context) {
        payPalClient = null
        payPalViewModel = null

        val coreConfig = CoreConfig(CLIENT_ID)
        PayPalWebCheckoutClient(context, coreConfig, "com.firstapp.paypaldemo").let { client ->
            payPalClient = client
            payPalViewModel = PayPalViewModel(client)
        }
    }

    /**
     * Start PayPal Checkout flow.
     * This is called from CartView’s “Pay with PayPal” button, for example.
     */
    fun startPayPalCheckout(activity: ComponentActivity, amount: Double) {
        val vm = payPalViewModel ?: return
        viewModelScope.launch {
            _checkoutState.value = CheckoutState.Loading("Starting PayPal Checkout")
            vm.startPayPalCheckout(
                amount = amount,
                activity = activity,
                onSuccess = {
                    // We successfully launched the web flow, now just wait for onNewIntent to “finish”.
                },
                onFailure = { error ->
                    _checkoutState.value = CheckoutState.Error(error)
                }
            )
        }
    }

    fun openPaymentLink(activity: ComponentActivity, uri: Uri) {
        val intent = CustomTabsIntent.Builder().build()
        intent.launchUrl(activity, uri)
    }

    private fun isAppSwitchUri(uri: Uri) = uri.host == DemoMerchantAPI.APP_SWITCH_HOST

    /**
     * Called from MainActivity.onNewIntent
     * to finish the PayPal flow after the Chrome Custom Tab returns.
     */
    fun handleOnNewIntent(intent: Intent) {
        val deepLinkUri = intent.data
        if (deepLinkUri != null && isAppSwitchUri(deepLinkUri)) {
            val isSuccessfulDeepLink = deepLinkUri.path?.contains("success") ?: false
            if (isSuccessfulDeepLink) {
                _checkoutState.value = CheckoutState.PaymentLinkComplete(deepLinkUri)
            } else {
                Log.d(TAG, "❌ Not a success URL")
            }
        } else {
            val vm = payPalViewModel ?: return
            viewModelScope.launch {
                _checkoutState.value = CheckoutState.Loading("Finishing PayPal Checkout")
                vm.finishPayPalCheckout(
                    intent = intent,
                    onSuccess = { completedOrderId ->
                        _checkoutState.value = CheckoutState.OrderComplete(completedOrderId)
                    },
                    onCanceled = {
                        _checkoutState.value = CheckoutState.Error("Checkout canceled by user.")
                    },
                    onFailure = { error ->
                        _checkoutState.value = CheckoutState.Error(error)
                    }
                )
            }
        }
    }

    // For final success
    fun onCardCheckoutComplete(orderId: String) {
        _checkoutState.value = CheckoutState.OrderComplete(orderId)
    }

    // If we want to show an error from the card flow
    fun showError(message: String) {
        _checkoutState.value = CheckoutState.Error(message)
    }


    /**
     * Reset or clear any error/completion state if user navigates away.
     */
    fun resetState() {
        _checkoutState.value = CheckoutState.Idle
        payPalClient = null
        payPalViewModel = null
    }

    override fun onCleared() {
        super.onCleared()
        payPalClient = null
        payPalViewModel = null
    }
}

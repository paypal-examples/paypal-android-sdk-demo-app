package com.firstapp.paypaldemo

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// These are for testing purposes. New orderID needs to be generated for each run
const val CLIENT_ID =
    "AQ04yLjwYNK_cZvD-S-HZY1TwV22AygaJ0JSiYdyqTcfcwRL6i8thQxKdTCZROmUou86wza_xoDk1WGz"

/**
 * Simple sealed class representing states we might show:
 *  - Idle (Cart)
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
}

/**
 * The coordinator ViewModel that orchestrates PayPal and Card payments.
 */
class CheckoutCoordinatorViewModel : ViewModel() {

    // The underlying PayPalWebCheckoutClient (depends on an Activity context).
    // We'll set it in onCreate of Activity via a setter, or lazy init.
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
    fun setActivityForPayPalClient(context: Context) {
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
    fun startPayPalCheckout(activity: ComponentActivity,  amount: Double) {
        val vm = payPalViewModel ?: return
        viewModelScope.launch {
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

    /**
     * Called from MainActivity.onNewIntent
     * to finish the PayPal flow after the Chrome Custom Tab returns.
     */
    fun handleOnNewIntent(intent: Intent) {
        val vm = payPalViewModel ?: return
        viewModelScope.launch {
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

    /**
     * Reset or clear any error/completion state if user navigates away.
     */
    fun resetState() {
        _checkoutState.value = CheckoutState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        payPalClient = null
        payPalViewModel = null
    }
}

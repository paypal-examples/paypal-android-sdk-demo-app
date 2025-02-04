package com.firstapp.paypaldemo.Main

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.firstapp.paypaldemo.CardCheckout.CardPaymentViewModel
import com.firstapp.paypaldemo.PayPalCheckout.PayPalViewModel
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.cardpayments.CardClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

const val CLIENT_ID = "AVhcAP8TDu5PFeAw97M8187g-iYQW8W0AhvvXaMaWPojJRGGkunX8r-fyPkKGCv09P83KC2dijKLKwyz"

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
    data class CardCheckout(val amount: Double) : CheckoutState()
    data class OrderComplete(val orderId: String) : CheckoutState()
    data class Error(val message: String) : CheckoutState()
}

/**
 * The coordinator ViewModel that orchestrates PayPal and Card payments.
 */
class CheckoutCoordinatorViewModel : ViewModel() {

    // The underlying PayPalWebCheckoutClient (depends on an Activity context).
    // We'll set it on PayPal button press from CartView
    private var payPalClient: PayPalWebCheckoutClient? = null

    // The underlying CardClient
    // We'll set it on Card button press from CartView
    private var cardClient: CardClient? = null

    private var cardPaymentViewModel: CardPaymentViewModel? = null

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
    fun startPayPalCheckout(activity: ComponentActivity,  amount: Double) {
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

    fun initializeCardClient(context: Context) {
        cardClient = null
        cardPaymentViewModel = null

        val coreConfig = CoreConfig(CLIENT_ID)
        CardClient(context, coreConfig).let { client ->
            cardClient = client
            cardPaymentViewModel = CardPaymentViewModel(client)
        }
    }

    fun startCardCheckout(amount: Double) {
        _checkoutState.value = CheckoutState.CardCheckout(amount)
    }


    /**
     * Called from MainActivity.onNewIntent
     * to finish the PayPal flow after the Chrome Custom Tab returns.
     */
    fun handleOnNewIntent(intent: Intent) {
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

    fun getCardPaymentViewModel(): CardPaymentViewModel? {
        return cardPaymentViewModel
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
        cardClient = null
        cardPaymentViewModel = null
    }

    override fun onCleared() {
        super.onCleared()
        payPalClient = null
        payPalViewModel = null
        cardClient = null
        cardPaymentViewModel = null
    }
}

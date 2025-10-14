package com.firstapp.paypaldemo.main

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import com.firstapp.paypaldemo.service.DemoMerchantAPI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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
    data class OrderCreateInProgress(val message: String = "Loading...") : CheckoutState()
    data class StartPayPalInProgress(val message: String) : CheckoutState()
    data class OrderComplete(val orderId: String) : CheckoutState()
    data class Error(val message: String) : CheckoutState()
    data class PaymentLinkComplete(val uri: Uri) : CheckoutState()
}

private const val TAG = "CheckoutCoordinatorViewModel"

/**
 * The coordinator ViewModel that orchestrates PayPal and Card payments.
 */
class CheckoutCoordinatorViewModel : ViewModel() {

    // Live state (or “flow state”) to help the UI know what to display.
    private val _checkoutState = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
    val checkoutState: StateFlow<CheckoutState> = _checkoutState

    /**
     * Reset or clear any error/completion state if user navigates away.
     */
    fun resetState() {
        _checkoutState.value = CheckoutState.Idle
    }
}

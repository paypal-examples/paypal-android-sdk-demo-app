package com.firstapp.paypaldemo.main

import android.net.Uri

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


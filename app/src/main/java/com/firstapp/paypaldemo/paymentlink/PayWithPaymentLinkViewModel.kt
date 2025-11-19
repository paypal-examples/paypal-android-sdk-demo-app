package com.firstapp.paypaldemo.paymentlink

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import com.firstapp.paypaldemo.Constants.SHOPPING_CART_ITEMS
import com.firstapp.paypaldemo.main.CartUiState
import com.firstapp.paypaldemo.main.CheckoutState
import com.firstapp.paypaldemo.service.DemoMerchantAPI
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "PayWithPaymentLinkViewModel"

@HiltViewModel
class PayWithPaymentLinkViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel() {

    private var _uiState = MutableStateFlow(defaultCartUiState)
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    private var checkoutState
        get() = _uiState.value.checkoutState
        set(value) = _uiState.update { prevState -> prevState.copy(checkoutState = value) }

    private fun isAppSwitchUri(uri: Uri) = uri.host == DemoMerchantAPI.APP_SWITCH_HOST

    /**
     * Called after the user returns from the Chrome Custom Tab to finish the checkout.
     */
    fun finishPayWithPaymentLink(intent: Intent?) {
        val deepLinkUri = intent?.data
        if (deepLinkUri != null && isAppSwitchUri(deepLinkUri)) {
            val isSuccessfulDeepLink = deepLinkUri.path?.contains("success") ?: false
            checkoutState = if (isSuccessfulDeepLink) {
                CheckoutState.PaymentLinkComplete(deepLinkUri)
            } else {
                CheckoutState.Error("Pay with Payment Link Unsuccessful")
            }
        } else {
            // update UI to show Retry button
            _uiState.update { currentState ->
                currentState.copy(checkoutState = CheckoutState.Idle, didInitiateCheckout = true)
            }
        }
    }

    fun launchUri(activity: ComponentActivity, uri: Uri) {
        checkoutState =
            CheckoutState.OrderCreateInProgress("Pay with Payment Link in Progress")
        val intent = CustomTabsIntent.Builder().build()
        intent.launchUrl(activity, uri)
    }

    companion object {
        private val defaultCartUiState by lazy {
            val items = SHOPPING_CART_ITEMS
            val totalAmount = items.sumOf { it.amount }
            CartUiState(
                items = items,
                totalAmount = totalAmount,
                checkoutState = CheckoutState.Idle,
                didInitiateCheckout = false
            )
        }
    }
}

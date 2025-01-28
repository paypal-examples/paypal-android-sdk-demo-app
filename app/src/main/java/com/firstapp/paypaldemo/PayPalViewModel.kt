package com.firstapp.paypaldemo

import androidx.activity.ComponentActivity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.paypal.android.paypalwebpayments.PayPalPresentAuthChallengeResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishStartResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFundingSource
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutRequest

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch



class PayPalViewModel(
private val payPalClient: PayPalWebCheckoutClient
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private var authState: String? = null

    private val _orderId = MutableStateFlow<String?>(null)
    val orderId: StateFlow<String?> = _orderId

    fun startPayPalCheckout(
        activity: ComponentActivity,
        orderId: String
    ) {
        viewModelScope.launch {
            val request = PayPalWebCheckoutRequest(
                orderId = orderId,
                fundingSource = PayPalWebCheckoutFundingSource.PAYPAL
            )

            when (val result = payPalClient.start(activity, request)) {
                is PayPalPresentAuthChallengeResult.Success -> {
                    authState = result.authState
                }
                is PayPalPresentAuthChallengeResult.Failure -> {
                    _errorMessage.value = result.error.toString()
                }
                else -> {
                    _errorMessage.value = "Unexpected error during checkout."
                }
            }
        }
    }

    fun finishPayPalCheckout(
        activity: ComponentActivity,
        intent: android.content.Intent
    ) {

        viewModelScope.launch {
            val result = authState?.let { authState ->
                payPalClient.finishStart(intent, authState)
            }
            when (result) {
                is PayPalWebCheckoutFinishStartResult.Success -> {
                   _orderId.value = result.orderId
                }

                is PayPalWebCheckoutFinishStartResult.Failure -> {
                    _errorMessage.value = result.error.toString()
                }

                is PayPalWebCheckoutFinishStartResult.Canceled -> {
                    _errorMessage.value = "Checkout was canceled by the user."
                }

                else -> {
                    _errorMessage.value = "Unexpected error occurred while completing the checkout."
                }
            }

        }
    }

    fun clearOrderId() {
        _orderId.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun getAuthState(): String? {
        return authState
    }

    fun setAuthState(state: String?) {
        authState = state
    }
}

class PayPalViewModelFactory(
    private val payPalClient: PayPalWebCheckoutClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PayPalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PayPalViewModel(payPalClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

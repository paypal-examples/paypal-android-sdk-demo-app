package com.firstapp.paypaldemo.cardcheckout

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.firstapp.paypaldemo.service.Amount
import com.firstapp.paypaldemo.service.DemoMerchantAPI
import com.firstapp.paypaldemo.service.PurchaseUnit
import com.paypal.android.cardpayments.CardClient
import com.paypal.android.cardpayments.Card
import com.paypal.android.cardpayments.CardApproveOrderResult
import com.paypal.android.cardpayments.CardRequest
import com.paypal.android.cardpayments.threedsecure.SCA
import kotlinx.coroutines.launch
import java.lang.Exception

/**
 * A separate ViewModel that:
 *  - Creates an order on your merchant server.
 *  - Approves that order with the PayPal card client (callback-based).
 *  - Captures the order on your server.
 *
 * No 3DS logic here: If a flow requires 3DS, it won't be handled.
 */
class CardPaymentViewModel(
    private val cardClient: CardClient
) : ViewModel() {

    var isLoading: Boolean = false
        private set
    var errorMessage: String = ""
        private set

    /**
     * The main “checkoutWithCard” function, using the validated Card details.
     */
    fun checkoutWithCard(
        activity: ComponentActivity,
        card: Card,
        amount: Double,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        isLoading = true
        errorMessage = ""

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
                val orderId = order.id
                println("✅ Created order: $orderId, status: ${order.status}")

                val sdkCard = Card(
                    number = card.number,
                    expirationMonth = card.expirationMonth,
                    expirationYear = card.expirationYear,
                    securityCode = card.securityCode
                )
                val request = CardRequest(
                    orderId,
                    sdkCard,
                    returnUrl = "com.firstapp.paypal.demo://example.com",
                    sca = SCA.SCA_WHEN_REQUIRED
                    )

                cardClient.approveOrder(request) { approveResult ->
                    when (approveResult) {
                        is CardApproveOrderResult.Success -> {
                            println("✅ Card approved. orderId=${approveResult.orderId}, status=${approveResult.status}")
                            // 3) Capture on your server
                            viewModelScope.launch {
                                try {
                                    val finalOrder =
                                        DemoMerchantAPI.completeOrder(orderId, "CAPTURE")
                                    println("✅ Order captured: ${finalOrder.id}, status=${finalOrder.status}")
                                    isLoading = false
                                    onSuccess(finalOrder.id)
                                } catch (e: Exception) {
                                    isLoading = false
                                    errorMessage = "Capture error: ${e.message}"
                                    onFailure(errorMessage)
                                }
                            }
                        }
                        is CardApproveOrderResult.AuthorizationRequired -> {
                            // 3DS flow required if the user had set sca, but we said “no 3ds,”
                            // so we can treat it as an error or skip handling it:
                            isLoading = false
                            errorMessage = "Auth required but 3DS not supported in this example."
                            onFailure(errorMessage)
                        }
                        is CardApproveOrderResult.Failure -> {
                            isLoading = false
                            errorMessage = "Card approve failed: ${approveResult.error.message}"
                            onFailure(errorMessage)
                        }
                    }
                }

            } catch (createErr: Exception) {
                // If the merchant server createOrder fails
                isLoading = false
                errorMessage = "Create order error: ${createErr.message}"
                onFailure(errorMessage)
            }
        }
    }
}

package com.firstapp.paypaldemo

import androidx.lifecycle.ViewModel
import com.paypal.android.cardpayments.Card

class CardCheckoutValidationViewModel : ViewModel() {

    var errorMessage: String = ""
    var cardNumber: String = "4111111111111111"
    var expirationDate: String = "0127"
    var cvv: String = "123"

    private val cardFormatter = CardFormatter()

    val isValid: Boolean
        get() = isCardFormValid()

    /**
     * Check basic length constraints:
     * - Card # must be 15-19 digits
     * - Expiration date must be exactly 4 digits (MMYY)
     * - CVV must be 3 or 4 digits
     */
    private fun isCardFormValid(): Boolean {
        val cleanedNumber = cardNumber
        val cleanedExp = expirationDate
        val cleanedCvv = cvv

        val numberOk = cleanedNumber.length in 15..19
        val expOk = (cleanedExp.length == 4)
        val cvvOk = (cleanedCvv.length in 3..4)

        return numberOk && expOk && cvvOk
    }

    /**
     * If valid, return a Card object we can send to `CardPaymentViewModel`.
     * Otherwise set errorMessage and return null.
     */
    fun isCardValid(): Card? {
        return if (isValid) {
            // create a Card object
            val cleanedNumber = cardNumber
            val exp = expirationDate
            val month = exp.take(2)
            val year = "20" + exp.drop(2)
            Card(
                number = cleanedNumber,
                expirationMonth = month,
                expirationYear = year,
                securityCode = cvv
            )
        } else {
            errorMessage = "Invalid card details. Please check and try again."
            null
        }
    }

    fun updateCardNumber(newValue: String) {
        cardNumber = cardFormatter.formatFieldWith(newValue, Fields.CARD_NUMBER)
        // optionally replicate iOS "cvv = 1234 if AMEX else 123"
        val type = CardType.getCardType(cardNumber)
        if (type == CardType.AMERICAN_EXPRESS && cvv.length < 4) {
            cvv = "1234"
        } else if (type != CardType.AMERICAN_EXPRESS && cvv.length > 3) {
            cvv = "123"
        }
    }

    fun updateExpirationDate(newValue: String) {
        val cleaned = newValue.filter { it.isDigit() }.take(4)
        expirationDate = cleaned
    }

    fun updateCVV(newValue: String) {
        cvv = cardFormatter.formatFieldWith(newValue, Fields.CVV)
    }
}
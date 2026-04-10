package com.firstapp.paypaldemo.cardcheckout

enum class CardFields {
    CARD_NUMBER,
    EXPIRATION_DATE,
    CVV
}

class CardFormatter {

    /**
     * Main entry point: choose how to fomrmat the string based on the field type.
     */
    fun formatFieldWith(text: String, field: CardFields): String {
        return when (field) {
            CardFields.CARD_NUMBER -> {
               text.filter { it.isDigit() }.take(19)
            }
            CardFields.EXPIRATION_DATE -> {
                text
            }
            CardFields.CVV -> {
                val cleaned = text.filter { it.isDigit() }
                cleaned.take(4)
            }
        }
    }

    /**
     * Insert spaces at the cardType's spaceDelimiterIndices.
     */
    private fun formatCardNumber(digits: String, cardType: CardType): String {
        val sb = StringBuilder(digits)
        // Insert space from left to right
        cardType.spaceDelimiterIndices.forEach { index ->
            if (index < sb.length) {
                sb.insert(index, " ")
            }
        }
        return sb.toString()
    }

    /**
     * If more than 2 digits, insert " / " after index 2 => "MM / YY"
     */
    private fun formatExpirationDate(digits: String): String {
        if (digits.length <= 2) {
            return digits // e.g. "01"
        }
        // e.g. "0125" => "01 / 25"
        val firstTwo = digits.take(2)
        val remainder = digits.drop(2)
        return "$firstTwo / $remainder"
    }
}

enum class CardType(
    val spaceDelimiterIndices: List<Int>,
    val maxLength: Int
) {

    AMERICAN_EXPRESS(
        spaceDelimiterIndices = listOf(4, 11),
        maxLength = 15
    ),
    VISA(
        spaceDelimiterIndices = listOf(4, 9, 14),
        maxLength = 16
    ),
    DISCOVER(
        spaceDelimiterIndices = listOf(4, 9, 14),
        maxLength = 19
    ),
    MASTERCARD(
        spaceDelimiterIndices = listOf(4, 9, 14),
        maxLength = 16
    ),
    UNKNOWN(
        spaceDelimiterIndices = listOf(4, 9, 14),
        maxLength = 16
    );

    companion object {
        /**
         * Determines the card type by checking prefixes
         */
        fun getCardType(cardNumber: String): CardType {
            val cleaned = cardNumber.replace(" ", "")

            // AMEX starts with 34 or 37
            if (cleaned.startsWith("34") || cleaned.startsWith("37")) {
                return AMERICAN_EXPRESS
            }
            // Visa starts with 4
            if (cleaned.startsWith("4")) {
                return VISA
            }
            // Discover starts with 6011 or 65
            if (cleaned.startsWith("6011") || cleaned.startsWith("65")) {
                return DISCOVER
            }
            // MasterCard: starts with 51, 52, 53, 54, or 55
            if (cleaned.length >= 2) {
                val firstTwo = cleaned.take(2).toIntOrNull()
                if (firstTwo != null && firstTwo in 51..55) {
                    return MASTERCARD
                }
            }
            // Otherwise, default to UNKNOWN
            return UNKNOWN
        }
    }
}
package com.firstapp.paypaldemo.CardCheckout

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class ExpirationDateVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        // Raw digits, e.g. "0125"
        val input = text.text

        val out = buildString {
            if (input.length <= 2) {
                // e.g. "01"
                append(input)
            } else {
                // e.g. "0125" => "01 / 25"
                append(input.substring(0, 2))
                append(" / ")
                append(input.substring(2))
            }
        }

        // Map cursor positions from raw digits to transformed output
        val offsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return when {
                    offset <= 2 -> offset
                    offset <= 4 -> offset + 3 // skip " / "
                    else -> out.length
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                return when {
                    offset <= 2 -> offset
                    offset <= 5 -> offset - 3
                    else -> input.length
                }
            }
        }

        return TransformedText(AnnotatedString(out), offsetTranslator)
    }
}
package com.firstapp.paypaldemo

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class CardNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // Raw digits: e.g. "4111111111111111"
        val raw = text.text

        // Build the transformed string. For every 4 digits, insert a space.
        // e.g. "4111111111111111" -> "4111 1111 1111 1111"
        val builder = StringBuilder()
        // We'll keep track of where each raw index ends up in the transformed text
        val offsetMap = IntArray(raw.length)
        var extraChars = 0

        for (i in raw.indices) {
            // After every 4 digits, except the first group,
            // insert a space before appending the next digit:
            if (i > 0 && i % 4 == 0) {
                builder.append(' ')
                extraChars++
            }
            builder.append(raw[i])
            // The position in the transformed string:
            offsetMap[i] = i + extraChars
        }

        val out = builder.toString()

        // offsetTranslator helps Compose map cursor positions
        // from raw text to transformed text (and back).
        val offsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return if (offset < raw.length) offsetMap[offset] else out.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                // We invert offsetMap by looking for the nearest index
                // whose offsetMap[i] >= offset
                if (offset <= 0) return 0
                for (i in offsetMap.indices) {
                    if (offsetMap[i] >= offset) {
                        return i
                    }
                }
                return raw.length
            }
        }

        return TransformedText(AnnotatedString(out), offsetTranslator)
    }
}
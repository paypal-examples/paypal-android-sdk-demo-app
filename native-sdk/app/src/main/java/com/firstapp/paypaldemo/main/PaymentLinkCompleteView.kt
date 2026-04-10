package com.firstapp.paypaldemo.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PaymentLinkCompleteView(amount: String, onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text("Order Complete", fontWeight = FontWeight.Bold, fontSize = 25.sp)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Thank you for your order! Your amount is $amount")
        Spacer(modifier = Modifier.weight(1.0f))
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Done")
        }
    }
}

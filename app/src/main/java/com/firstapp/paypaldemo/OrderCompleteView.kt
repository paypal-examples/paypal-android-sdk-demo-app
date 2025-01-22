package com.firstapp.paypaldemo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun OrderCompleteView (
    orderID: String,
    onDone: () -> Unit
) {

    Column(
        modifier = Modifier
            .padding(40.dp)
        .padding(top = 48.dp),

        verticalArrangement = Arrangement.Bottom,
    ) {
        Text("Order Complete", fontWeight = FontWeight.Bold, fontSize = 25.sp)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Thank you for your order!")
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

@ExperimentalMaterial3Api
@Preview
@Composable
fun OrderCompleteViewPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            OrderCompleteView(
                orderID = "test",
                onDone = {
                    println("OrderComplete!")
                }
            )
        }
    }
}

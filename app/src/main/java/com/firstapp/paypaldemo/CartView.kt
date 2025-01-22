package com.firstapp.paypaldemo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.foundation.BorderStroke

data class Item(
    val name: String,
    val amount: Double,
    val imageResId: Int
)

@Composable
fun CartView(
    onPayWithPayPal: (Double) -> Unit,
    onPayWithCard: (Double) -> Unit
) {
    val items = listOf(Item(name = "White T-shirt", amount = 29.99, imageResId = R.drawable.tshirt ))
    val totalAmount = items.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(top = 48.dp),

        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Cart",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 25.dp)
        )

        items.forEach { item ->
            CartItemView(item)
        }

        Divider(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .fillMaxWidth(),
            color = Color.Gray.copy(alpha = 0.2f)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Total",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$${"%.2f".format(totalAmount)}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .padding(16.dp)
                .padding(bottom = 40.dp)
        ) {
            PaymentButton(
                text = "Pay with PayPal",
                backgroundColor = Color(0xFFFFB700),
                onClick = { onPayWithPayPal(totalAmount) }
            )
            Spacer(modifier = Modifier.height(10.dp))
            PaymentButton(
                text = "Pay with Card",
                backgroundColor = Color.Black,
                onClick = { onPayWithCard(totalAmount) }
            )
        }
    }
}

@Composable
fun CartItemView(item: Item) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = item.imageResId),
                    contentDescription = item.name,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Gray.copy(alpha = 0.2f))
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "$${"%.2f".format(item.amount)}",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PaymentButton(
    text: String,
    backgroundColor: Color,
    iconRes: Int? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (text.contains("PayPal")) {
                Image(
                    painter = painterResource(id = R.drawable.paypal_color_monogram3x),
                    contentDescription = "PayPal logo",
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp)
                )
            }
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
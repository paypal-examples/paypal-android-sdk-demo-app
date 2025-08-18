package com.firstapp.paypaldemo.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.firstapp.paypaldemo.R
import com.paypal.android.paymentbuttons.PayPalButton
import com.paypal.android.paymentbuttons.PayPalButtonColor
import com.paypal.android.paymentbuttons.PayPalButtonLabel
import com.paypal.android.paymentbuttons.PaymentButtonSize

data class Item(
    val name: String,
    val amount: Double,
    val imageResId: Int
)

@ExperimentalMaterial3Api
@Composable
fun CartView(
    onPayWithPayPal: (Double) -> Unit,
    onPayWithCard: (Double) -> Unit,
    onPayWithLink: (Double) -> Unit
) {
    val payPalButtonCornerRadius = with(LocalDensity.current) { 10.dp.toPx() }

    val items = listOf(Item(name = "10 Credit Points", amount = 19.99, imageResId = R.drawable.gold))
    val totalAmount = items.sumOf { it.amount }

    var isPaymentLinkEnabled by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp, start = 20.dp, end = 20.dp),

        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = isPaymentLinkEnabled,
                onClick = {
                    isPaymentLinkEnabled = true
                },
                shape = SegmentedButtonDefaults.itemShape(index = 0, 2)
            ) {
                Text("Use Payment Link")
            }
            SegmentedButton(
                selected = !isPaymentLinkEnabled,
                onClick = {
                    isPaymentLinkEnabled = false
                },
                shape = SegmentedButtonDefaults.itemShape(index = 1, 2)
            ) {
                Text("Use PayPal SDK")
            }
        }
        Text(
            text = "Cart",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp, bottom = 25.dp)
        )

        items.forEach { item ->
            CartItemView(item)
        }

        HorizontalDivider(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth(),
            color = Color.Gray.copy(alpha = 0.2f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth(),
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
        if (isPaymentLinkEnabled) {
            PaymentButton(
                text = "Pay Now",
                backgroundColor = Color.Black,
                onClick = {
                    onPayWithLink(totalAmount)
                }
            )

        } else {
            AndroidView(
                factory = { context ->
                    PayPalButton(context).apply { setOnClickListener { onPayWithPayPal(totalAmount) } }
                },
                update = { button ->
                    button.color = PayPalButtonColor.BLUE
                    button.label = PayPalButtonLabel.PAY
                    button.size = PaymentButtonSize.LARGE
                    button.customCornerRadius = payPalButtonCornerRadius
                },
                modifier = Modifier.fillMaxWidth()
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
            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@ExperimentalMaterial3Api
@Preview
@Composable
fun CartViewPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            CartView(
                onPayWithCard = {},
                onPayWithPayPal = {},
                onPayWithLink = {}
            )
        }
    }
}

package com.firstapp.paypaldemo

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.paypalwebpayments.PayPalPresentAuthChallengeResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishStartResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutRequest

data class Item(
    val name: String,
    val amount: Double,
    val imageResId: Int
)

const val CLIENT_ID =
    "AQ04yLjwYNK_cZvD-S-HZY1TwV22AygaJ0JSiYdyqTcfcwRL6i8thQxKdTCZROmUou86wza_xoDk1WGz"
const val ORDER_ID = "88L77523PP469242W"

@Composable
fun CartView(
//    onPayWithPayPal: (Double) -> Unit,
    onPayPalPaymentSuccess: (orderId: String) -> Unit,
    onPayWithCard: (Double) -> Unit
) {
    var authState by rememberSaveable(key = "auth_state") { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val payPalClient = remember {
        // remember PayPalClient so that it only gets created during the initial composition
        val config = CoreConfig(CLIENT_ID, environment = Environment.SANDBOX)
        PayPalWebCheckoutClient(context, config, "com.paypal.android.demo")
    }

    OnLifecycleOwnerResumeEffect {
        context.getActivityOrNull()?.intent?.let { intent ->
            authState?.let { pendingState ->
                when (val result = payPalClient.finishStart(intent, pendingState)) {
                    is PayPalWebCheckoutFinishStartResult.Success -> onPayPalPaymentSuccess(ORDER_ID)
                    is PayPalWebCheckoutFinishStartResult.Failure -> TODO("handle failure")
                    is PayPalWebCheckoutFinishStartResult.Canceled -> TODO("handle canceled")
                    PayPalWebCheckoutFinishStartResult.NoResult -> {
                        // do nothing
                    }
                }
            }
        }
    }

    OnNewIntentEffect { newIntent ->
        authState?.let { pendingState ->
            when (val result = payPalClient.finishStart(newIntent, pendingState)) {
                is PayPalWebCheckoutFinishStartResult.Success -> onPayPalPaymentSuccess(ORDER_ID)
                is PayPalWebCheckoutFinishStartResult.Failure -> TODO("handle failure")
                is PayPalWebCheckoutFinishStartResult.Canceled -> TODO("handle canceled")
                PayPalWebCheckoutFinishStartResult.NoResult -> {
                    // do nothing
                }
            }
        }
    }

    val items = listOf(Item(name = "White T-shirt", amount = 29.99, imageResId = R.drawable.tshirt))
    val totalAmount = items.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),

        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Cart",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 25.dp)
        )

        items.forEach { item ->
            CartItemView(item)
        }

        Divider(
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

        PaymentButton(
            text = "Pay with PayPal",
            backgroundColor = Color(0xFFFFB700),
            onClick = {
                // launch PayPal Web Checkout Flow
                context.getActivityOrNull()?.let { activity ->
                    val request = PayPalWebCheckoutRequest(ORDER_ID)
                    when (val result = payPalClient.start(activity, request)) {
                        is PayPalPresentAuthChallengeResult.Success -> {
                            authState = result.authState
                        }

                        is PayPalPresentAuthChallengeResult.Failure -> TODO("handle error")
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(10.dp))
        PaymentButton(
            text = "Pay with Card",
            backgroundColor = Color.Black,
            onClick = { onPayWithCard(totalAmount) }
        )
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
                onPayWithCard = { amount -> },
                onPayPalPaymentSuccess = {},
//                onPayWithPayPal = { amount -> }
            )
        }
    }
}

@Composable
fun OnLifecycleOwnerResumeEffect(callback: () -> Unit) {
    // Ref: https://stackoverflow.com/a/66549433
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            callback()
        }
    }
}

@Composable
fun OnNewIntentEffect(callback: (newIntent: Intent) -> Unit) {
    val context = LocalContext.current
    // pass "Unit" to register listener only once
    DisposableEffect(Unit) {
        val listener = Consumer<Intent> { newIntent ->
            callback(newIntent)
        }
        context.getActivityOrNull()?.addOnNewIntentListener(listener)
        onDispose {
            context.getActivityOrNull()?.removeOnNewIntentListener(listener)
        }
    }
}

// Ref: https://stackoverflow.com/a/68423182
fun Context.getActivityOrNull(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.getActivityOrNull()
    else -> null
}
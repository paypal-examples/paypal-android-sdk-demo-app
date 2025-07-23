package com.firstapp.paypaldemo.cardcheckout

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.ComponentActivity
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * A composable for card checkout
 */
@Composable
fun CardCheckoutView(
    amount: Double,
    onOrderCompleted: (String) -> Unit,
    cardPaymentViewModel: CardPaymentViewModel = hiltViewModel()
) {
    // We'll create or retrieve a validation VM
    val validationVM = remember { CardCheckoutValidationViewModel() }
    // Retrieve the cardPaymentViewModel from coordinator

    // We also need the current Activity to pass to checkoutWithCard:
    val activity = LocalActivityResultRegistryOwner.current as ComponentActivity

    // Observe states from validationVM
    var cardNumber by rememberSaveable { mutableStateOf(validationVM.cardNumber) }
    var expirationDate by rememberSaveable { mutableStateOf(validationVM.expirationDate) }
    var cvv by rememberSaveable { mutableStateOf(validationVM.cvv) }

    // For showing a loading overlay or error
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }

    // Sync fields each time user edits them
    fun refreshFieldsFromVM() {
        cardNumber = validationVM.cardNumber
        expirationDate = validationVM.expirationDate
        cvv = validationVM.cvv
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {

            Text(
                text = "Card Checkout",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Card Number
            OutlinedTextField(
                value = cardNumber,
                onValueChange = { newVal ->
                    validationVM.updateCardNumber(newVal)
                    refreshFieldsFromVM()
                },
                visualTransformation =  CardNumberVisualTransformation(),
                label = { Text("Card Number") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Expiration + CVV
            Row {
                OutlinedTextField(
                    value = expirationDate,
                    onValueChange = { newVal ->
                        validationVM.updateExpirationDate(newVal)
                        refreshFieldsFromVM()
                    },
                    label = { Text("MM / YY") },
                    visualTransformation = ExpirationDateVisualTransformation(),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                OutlinedTextField(
                    value = cvv,
                    onValueChange = { newVal ->
                        validationVM.updateCVV(newVal)
                        refreshFieldsFromVM()
                    },
                    label = { Text("CVV") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = {
                    // 1) Validate
                    val card = validationVM.validCard()
                    if (card == null) {
                        errorMessage = validationVM.errorMessage
                        return@Button
                    }
                    // 2) If valid, call cardPaymentViewModel to do the flow
                    isLoading = true
                    if (cardPaymentViewModel != null) {
                        cardPaymentViewModel.checkoutWithCard(
                            card = card,
                            amount = amount,
                            onSuccess = { orderId ->
                                isLoading = false
                                onOrderCompleted(orderId)
                            }
                        ) { err ->
                            isLoading = false
                            errorMessage = err
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submit")
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            }
        }

        if (isLoading) {
            // Loading overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}


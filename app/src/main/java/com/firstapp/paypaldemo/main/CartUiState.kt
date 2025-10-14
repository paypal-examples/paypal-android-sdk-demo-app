package com.firstapp.paypaldemo.main

data class CartUiState(
    val items: List<Item>,
    val totalAmount: Double,
    val checkoutState: CheckoutState,
    val didInitiateCheckout: Boolean
)

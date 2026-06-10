package com.firstapp.paypaldemo

import com.firstapp.paypaldemo.main.Item

object Constants {
    const val DEEP_LINK_URL_SCHEME = "com.firstapp.paypaldemo"
    const val CLIENT_ID =
        "AQTfw2irFfemo-eWG4H5UY-b9auKihUpXQ2Engl4G1EsHJe2mkpfUv_SN3Mba0v3CfrL6Fk_ecwv9EOo"

    // NOTE: The shopping cart in this example is static. This code snippet should draw a parallel
    // to the data layer in your own application
    val SHOPPING_CART_ITEMS =
        listOf(Item(name = "10 Credit Points", amount = 19.99, imageResId = R.drawable.gold))
}
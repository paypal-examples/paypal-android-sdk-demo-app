package com.firstapp.paypaldemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.firstapp.paypaldemo.main.CheckoutFlow
import com.firstapp.paypaldemo.ui.theme.PayPalDemoTheme
import dagger.hilt.android.AndroidEntryPoint

const val CLIENT_ID =
    "AQTfw2irFfemo-eWG4H5UY-b9auKihUpXQ2Engl4G1EsHJe2mkpfUv_SN3Mba0v3CfrL6Fk_ecwv9EOo"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PayPalDemoTheme {
                Scaffold { innerPadding ->
                    CheckoutFlow(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

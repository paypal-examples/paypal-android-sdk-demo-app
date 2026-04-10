package com.example.appswitch

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread

/**
 * MainActivity - Single activity demonstrating PayPal App Switch Direct API.
 *
 * Uses android:launchMode="singleTop" so that onNewIntent() is called
 * when the buyer returns from PayPal (instead of creating a new activity instance).
 *
 * Two flows:
 * 1. "Pay $25.00" - One-time payment via Orders v2 with app_switch_context
 * 2. "Save PayPal" - Vault flow via Vault v3 with app_switch_context
 *
 * Source: https://developer.paypal.com/docs/business/payments/app-switch/direct-api
 */
class MainActivity : AppCompatActivity() {

    private lateinit var credentialStatusText: TextView
    private lateinit var statusText: TextView
    private lateinit var resultText: TextView
    private lateinit var payButton: Button
    private lateinit var vaultButton: Button
    private lateinit var progressBar: ProgressBar

    // Track which flow is active so we handle the return correctly
    private var activeFlow: String? = null

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        credentialStatusText = findViewById(R.id.credentialStatusText)
        statusText = findViewById(R.id.statusText)
        resultText = findViewById(R.id.resultText)
        payButton = findViewById(R.id.payButton)
        vaultButton = findViewById(R.id.vaultButton)
        progressBar = findViewById(R.id.progressBar)

        payButton.setOnClickListener { startPaymentFlow() }
        vaultButton.setOnClickListener { startVaultFlow() }

        // Initialize PayPalService with credentials from BuildConfig (injected from local.properties)
        PayPalService.initialize(
            clientId = BuildConfig.PAYPAL_CLIENT_ID,
            clientSecret = BuildConfig.PAYPAL_CLIENT_SECRET,
            returnDomain = BuildConfig.RETURN_DOMAIN
        )

        // Validate credentials on startup
        validateCredentials()

        // Check for return from PayPal on initial launch (cold start via app link)
        intent?.data?.let { uri ->
            handlePayPalReturn(uri)
        }
    }

    /**
     * Called when the activity is already running and receives a new intent.
     * This is the primary return path for app switch - PayPal redirects back
     * via the App Link which triggers onNewIntent since launchMode="singleTop".
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.let { uri ->
            handlePayPalReturn(uri)
        }
    }

    // ── One-Time Payment Flow ──────────────────────────────────────────

    private fun startPaymentFlow() {
        activeFlow = "orders"
        setLoading(true)
        updateStatus("Creating order...")

        thread {
            try {
                // Step 0: Get access token
                PayPalService.getAccessToken()

                // Step 2: Create order with app_switch_context
                val osType = "ANDROID"
                val osVersion = Build.VERSION.RELEASE
                val orderResponse = PayPalService.createOrder("25.00", osType, osVersion)

                val orderId = orderResponse.getString("id")
                val status = orderResponse.getString("status")

                // Check app_switch_eligibility from response
                val appSwitchEligible = orderResponse
                    .optJSONObject("payment_source")
                    ?.optJSONObject("paypal")
                    ?.optBoolean("app_switch_eligibility", false) ?: false

                // Extract payer-action URL from links array
                val links = parseLinks(orderResponse.getJSONArray("links"))
                val payerActionUrl = AppSwitchHandler.extractPayerActionUrl(links, "orders")

                runOnUiThread {
                    updateStatus("Order $orderId created (status: $status)\n" +
                            "App switch eligible: $appSwitchEligible")
                    setLoading(false)

                    if (payerActionUrl != null) {
                        // Step 3: Redirect buyer to PayPal
                        updateStatus("Opening PayPal for approval...")
                        AppSwitchHandler.handlePayerAction(this, payerActionUrl, appSwitchEligible)
                    } else {
                        updateStatus("ERROR: No payer-action URL in response")
                        appendResult("Order ID: $orderId\nStatus: $status\nMissing payer-action link")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Payment flow error", e)
                runOnUiThread {
                    setLoading(false)
                    updateStatus("Error: ${e.message}")
                }
            }
        }
    }

    // ── Vault Flow ─────────────────────────────────────────────────────

    private fun startVaultFlow() {
        activeFlow = "vault"
        setLoading(true)
        updateStatus("Creating setup token...")

        thread {
            try {
                PayPalService.getAccessToken()

                // Step 2: Create setup token with app_switch_context
                val osType = "ANDROID"
                val osVersion = Build.VERSION.RELEASE
                val setupResponse = PayPalService.createSetupToken(osType, osVersion)

                val setupTokenId = setupResponse.getString("id")
                val status = setupResponse.getString("status")

                val appSwitchEligible = setupResponse
                    .optJSONObject("payment_source")
                    ?.optJSONObject("paypal")
                    ?.optBoolean("app_switch_eligibility", false) ?: false

                // For vault, the link rel is "approve" (not "payer-action")
                val links = parseLinks(setupResponse.getJSONArray("links"))
                val approveUrl = AppSwitchHandler.extractPayerActionUrl(links, "vault")

                runOnUiThread {
                    updateStatus("Setup token $setupTokenId created (status: $status)\n" +
                            "App switch eligible: $appSwitchEligible")
                    setLoading(false)

                    if (approveUrl != null) {
                        updateStatus("Opening PayPal for vault approval...")
                        AppSwitchHandler.handlePayerAction(this, approveUrl, appSwitchEligible)
                    } else {
                        updateStatus("ERROR: No approve URL in response")
                        appendResult("Setup Token: $setupTokenId\nStatus: $status\nMissing approve link")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Vault flow error", e)
                runOnUiThread {
                    setLoading(false)
                    updateStatus("Error: ${e.message}")
                }
            }
        }
    }

    // ── Return Handling ────────────────────────────────────────────────

    /**
     * Step 4: Handle the return from PayPal.
     *
     * From doc:
     * - Orders flow: PayPal returns token + PayerID -> capture the order
     * - Vault flow: PayPal returns approval_session_id (or token_id) -> create payment token
     */
    private fun handlePayPalReturn(uri: Uri) {
        Log.d(TAG, "PayPal return URL: $uri")
        updateStatus("PayPal returned. Processing...")
        setLoading(true)

        when (val result = AppSwitchHandler.parseReturnUrl(uri)) {
            is AppSwitchHandler.ReturnResult.OrderApproved -> {
                updateStatus("Order approved! Capturing payment...\n" +
                        "Order: ${result.orderId}, Payer: ${result.payerId}")
                captureApprovedOrder(result.orderId)
            }
            is AppSwitchHandler.ReturnResult.VaultApproved -> {
                updateStatus("Vault approved! Creating payment token...\n" +
                        "Setup Token: ${result.setupTokenId}")
                finalizeVault(result.setupTokenId)
            }
            is AppSwitchHandler.ReturnResult.Unknown -> {
                setLoading(false)
                updateStatus("Unrecognized return URL: ${result.rawUrl}")
            }
        }
    }

    private fun captureApprovedOrder(orderId: String) {
        thread {
            try {
                val captureResponse = PayPalService.captureOrder(orderId)
                val captureStatus = captureResponse.optString("status", "UNKNOWN")

                // Extract capture ID from the response
                var captureId = "N/A"
                val purchaseUnits = captureResponse.optJSONArray("purchase_units")
                if (purchaseUnits != null && purchaseUnits.length() > 0) {
                    val payments = purchaseUnits.getJSONObject(0).optJSONObject("payments")
                    val captures = payments?.optJSONArray("captures")
                    if (captures != null && captures.length() > 0) {
                        captureId = captures.getJSONObject(0).optString("id", "N/A")
                    }
                }

                runOnUiThread {
                    setLoading(false)
                    updateStatus("Payment Complete!")
                    appendResult(
                        "--- Payment Result ---\n" +
                        "Order ID: $orderId\n" +
                        "Capture ID: $captureId\n" +
                        "Status: $captureStatus"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Capture error", e)
                runOnUiThread {
                    setLoading(false)
                    updateStatus("Capture failed: ${e.message}")
                }
            }
        }
    }

    private fun finalizeVault(setupTokenId: String) {
        thread {
            try {
                val tokenResponse = PayPalService.createPaymentToken(setupTokenId)
                val paymentTokenId = tokenResponse.optString("id", "N/A")
                val vaultStatus = tokenResponse.optString("status", "UNKNOWN")

                runOnUiThread {
                    setLoading(false)
                    updateStatus("Vault Complete!")
                    appendResult(
                        "--- Vault Result ---\n" +
                        "Setup Token: $setupTokenId\n" +
                        "Payment Token (vault_id): $paymentTokenId\n" +
                        "Status: $vaultStatus\n\n" +
                        "Use this vault_id for future charges\n" +
                        "without buyer re-authentication."
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Vault finalization error", e)
                runOnUiThread {
                    setLoading(false)
                    updateStatus("Vault failed: ${e.message}")
                }
            }
        }
    }

    // ── Credential Validation ──────────────────────────────────────────

    private fun validateCredentials() {
        thread {
            val result = PayPalService.validateCredentials()
            runOnUiThread {
                result.onSuccess { message ->
                    credentialStatusText.text = "\u2705 $message"
                    credentialStatusText.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
                    credentialStatusText.setBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"))
                }
                result.onFailure { error ->
                    credentialStatusText.text = "\u274C Missing credentials \u2014 see README"
                    credentialStatusText.setTextColor(android.graphics.Color.parseColor("#C62828"))
                    credentialStatusText.setBackgroundColor(android.graphics.Color.parseColor("#FFEBEE"))
                    Log.e(TAG, "Credential validation failed: ${error.message}")
                }
            }
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private fun parseLinks(linksArray: JSONArray): List<Map<String, String>> {
        val links = mutableListOf<Map<String, String>>()
        for (i in 0 until linksArray.length()) {
            val link = linksArray.getJSONObject(i)
            links.add(mapOf(
                "rel" to link.optString("rel", ""),
                "href" to link.optString("href", ""),
                "method" to link.optString("method", "")
            ))
        }
        return links
    }

    private fun updateStatus(message: String) {
        statusText.text = message
    }

    private fun appendResult(message: String) {
        resultText.text = message
        resultText.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        payButton.isEnabled = !loading
        vaultButton.isEnabled = !loading
    }
}

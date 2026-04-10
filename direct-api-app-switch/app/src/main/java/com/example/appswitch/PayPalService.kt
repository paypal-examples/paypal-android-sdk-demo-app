package com.example.appswitch

import android.os.Build
import android.util.Base64
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * PayPalService - Server-side API calls for App Switch Direct API integration.
 *
 * IMPORTANT: In production, these calls MUST happen on your backend server.
 * Client ID and Secret are hardcoded here for sandbox testing only.
 *
 * Source: https://developer.paypal.com/docs/business/payments/app-switch/direct-api
 */
object PayPalService {

    private const val TAG = "PayPalService"
    private const val BASE_URL = "https://api-m.sandbox.paypal.com"

    // Loaded from local.properties (not checked into source control)
    // See README for setup instructions
    private var CLIENT_ID = ""
    private var CLIENT_SECRET = ""
    private var RETURN_DOMAIN = "https://example.com"

    /**
     * Initialize credentials from BuildConfig.
     * Call from MainActivity.onCreate() before any API calls.
     */
    fun initialize(clientId: String, clientSecret: String, returnDomain: String = "https://example.com") {
        CLIENT_ID = clientId
        CLIENT_SECRET = clientSecret
        // On emulator, use custom URL scheme so Chrome Custom Tabs can redirect
        // back to the app without domain verification (App Links).
        // On real devices, use the HTTPS domain from local.properties.
        val isEmulator = android.os.Build.FINGERPRINT.contains("generic") ||
                         android.os.Build.FINGERPRINT.contains("emulator") ||
                         android.os.Build.MODEL.contains("Emulator") ||
                         android.os.Build.MODEL.contains("Android SDK") ||
                         android.os.Build.MODEL.contains("sdk_gphone") ||
                         android.os.Build.HARDWARE == "ranchu" ||
                         android.os.Build.HARDWARE == "goldfish" ||
                         android.os.Build.PRODUCT.contains("sdk")
        RETURN_DOMAIN = if (isEmulator) {
            Log.d("PayPalService", "Emulator detected — using custom URL scheme for return URLs")
            "appswitch-test://callback"
        } else {
            returnDomain
        }
    }

    private var cachedAccessToken: String? = null

    /**
     * Step 0: Get an OAuth 2.0 access token.
     * POST /v1/oauth2/token
     */
    fun getAccessToken(): String {
        cachedAccessToken?.let { return it }

        val url = URL("$BASE_URL/v1/oauth2/token")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

        val credentials = "$CLIENT_ID:$CLIENT_SECRET"
        val encoded = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
        connection.setRequestProperty("Authorization", "Basic $encoded")

        connection.doOutput = true
        val writer = OutputStreamWriter(connection.outputStream)
        writer.write("grant_type=client_credentials")
        writer.flush()
        writer.close()

        val response = readResponse(connection)
        Log.d(TAG, "Token response: $response")

        val json = JSONObject(response)
        val token = json.getString("access_token")
        cachedAccessToken = token
        return token
    }

    /**
     * Step 2 (Orders): Create an order with app_switch_context.
     * POST /v2/checkout/orders
     *
     * The app_switch_context.native_app block opts into app switch.
     * return_url / cancel_url are web fallbacks when app switch is unavailable.
     *
     * @param amount Payment amount in USD
     * @param osType "ANDROID" for Android devices
     * @param osVersion Device OS version string (used for telemetry)
     * @return JSONObject with order ID, payer-action URL, and app_switch_eligibility
     */
    fun createOrder(amount: String, osType: String, osVersion: String): JSONObject {
        val accessToken = getAccessToken()

        val body = JSONObject().apply {
            put("intent", "CAPTURE")
            put("payment_source", JSONObject().apply {
                put("paypal", JSONObject().apply {
                    put("experience_context", JSONObject().apply {
                        put("user_action", "PAY_NOW")
                        put("return_url", "$RETURN_DOMAIN/checkout/success")
                        put("cancel_url", "$RETURN_DOMAIN/checkout/cancel")
                        put("app_switch_context", JSONObject().apply {
                            put("native_app", JSONObject().apply {
                                put("return_app_url", "$RETURN_DOMAIN/app-link/return")
                                put("cancel_app_url", "$RETURN_DOMAIN/app-link/cancel")
                                put("os_type", osType)
                                put("os_version", osVersion)
                            })
                        })
                    })
                })
            })
            put("purchase_units", JSONArray().apply {
                put(JSONObject().apply {
                    put("amount", JSONObject().apply {
                        put("currency_code", "USD")
                        put("value", amount)
                    })
                })
            })
        }

        val url = URL("$BASE_URL/v2/checkout/orders")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("PayPal-Request-Id", "order-${UUID.randomUUID()}")
        connection.setRequestProperty("Prefer", "return=representation")
        connection.doOutput = true

        val writer = OutputStreamWriter(connection.outputStream)
        writer.write(body.toString())
        writer.flush()
        writer.close()

        val response = readResponse(connection)
        Log.d(TAG, "Create order response: $response")
        return JSONObject(response)
    }

    /**
     * Step 4 (Orders): Capture an approved order.
     * POST /v2/checkout/orders/{orderId}/capture
     *
     * @param orderId The order ID (token) returned from the PayPal redirect
     * @return JSONObject with capture status, capture ID, and payment details
     */
    fun captureOrder(orderId: String): JSONObject {
        val accessToken = getAccessToken()

        val url = URL("$BASE_URL/v2/checkout/orders/$orderId/capture")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("PayPal-Request-Id", "capture-${UUID.randomUUID()}")
        connection.setRequestProperty("Prefer", "return=representation")
        connection.doOutput = true
        // Empty body for capture
        val writer = OutputStreamWriter(connection.outputStream)
        writer.write("")
        writer.flush()
        writer.close()

        val response = readResponse(connection)
        Log.d(TAG, "Capture response: $response")
        return JSONObject(response)
    }

    /**
     * Step 2 (Vault): Create a setup token with app_switch_context.
     * POST /v3/vault/setup-tokens
     *
     * @param osType "ANDROID" for Android devices
     * @param osVersion Device OS version string (used for telemetry)
     * @return JSONObject with setup token ID, approve URL, and app_switch_eligibility
     */
    fun createSetupToken(osType: String, osVersion: String): JSONObject {
        val accessToken = getAccessToken()

        val body = JSONObject().apply {
            put("payment_source", JSONObject().apply {
                put("paypal", JSONObject().apply {
                    put("description", "Save your PayPal account for faster checkout")
                    put("experience_context", JSONObject().apply {
                        put("user_action", "SETUP_NOW")
                        put("return_url", "$RETURN_DOMAIN/checkout/success?vaultFlow=true")
                        put("cancel_url", "$RETURN_DOMAIN/checkout/cancel?vaultFlow=true")
                        put("app_switch_context", JSONObject().apply {
                            put("native_app", JSONObject().apply {
                                put("return_app_url", "$RETURN_DOMAIN/app-link/return?vaultFlow=true")
                                put("cancel_app_url", "$RETURN_DOMAIN/app-link/cancel?vaultFlow=true")
                                put("os_type", osType)
                                put("os_version", osVersion)
                            })
                        })
                    })
                    put("usage_type", "MERCHANT")
                    put("customer_type", "CONSUMER")
                })
            })
        }

        val url = URL("$BASE_URL/v3/vault/setup-tokens")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("PayPal-Request-Id", "vault-${UUID.randomUUID()}")
        connection.doOutput = true

        val writer = OutputStreamWriter(connection.outputStream)
        writer.write(body.toString())
        writer.flush()
        writer.close()

        val response = readResponse(connection)
        Log.d(TAG, "Setup token response: $response")
        return JSONObject(response)
    }

    /**
     * Step 4 (Vault): Create a payment token from an approved setup token.
     * POST /v3/vault/payment-tokens
     *
     * @param setupTokenId The setup token ID (approval_session_id from the redirect)
     * @return JSONObject with payment token ID and VAULTED status
     */
    fun createPaymentToken(setupTokenId: String): JSONObject {
        val accessToken = getAccessToken()

        val body = JSONObject().apply {
            put("payment_source", JSONObject().apply {
                put("token", JSONObject().apply {
                    put("id", setupTokenId)
                    put("type", "SETUP_TOKEN")
                })
            })
        }

        val url = URL("$BASE_URL/v3/vault/payment-tokens")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("PayPal-Request-Id", "vault-token-${UUID.randomUUID()}")
        connection.doOutput = true

        val writer = OutputStreamWriter(connection.outputStream)
        writer.write(body.toString())
        writer.flush()
        writer.close()

        val response = readResponse(connection)
        Log.d(TAG, "Payment token response: $response")
        return JSONObject(response)
    }

    /**
     * Charge a vaulted PayPal account for a future payment.
     * POST /v2/checkout/orders (with vault_id instead of app_switch_context)
     *
     * @param vaultId The payment token ID from a previous vault flow
     * @param amount Payment amount in USD
     * @return JSONObject with order ID and status
     */
    fun chargeWithVaultId(vaultId: String, amount: String): JSONObject {
        val accessToken = getAccessToken()

        val body = JSONObject().apply {
            put("intent", "CAPTURE")
            put("payment_source", JSONObject().apply {
                put("paypal", JSONObject().apply {
                    put("vault_id", vaultId)
                })
            })
            put("purchase_units", JSONArray().apply {
                put(JSONObject().apply {
                    put("amount", JSONObject().apply {
                        put("currency_code", "USD")
                        put("value", amount)
                    })
                })
            })
        }

        val url = URL("$BASE_URL/v2/checkout/orders")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("PayPal-Request-Id", "vault-charge-${UUID.randomUUID()}")
        connection.setRequestProperty("Prefer", "return=representation")
        connection.doOutput = true

        val writer = OutputStreamWriter(connection.outputStream)
        writer.write(body.toString())
        writer.flush()
        writer.close()

        val response = readResponse(connection)
        Log.d(TAG, "Vault charge response: $response")
        return JSONObject(response)
    }

    /**
     * Validate sandbox credentials on app launch.
     * Checks that credentials are configured and can authenticate.
     *
     * @return Result.success with connection message, or Result.failure with descriptive error
     */
    fun validateCredentials(): Result<String> {
        if (CLIENT_ID.isEmpty() || CLIENT_SECRET.isEmpty()) {
            return Result.failure(
                Exception("Missing credentials. Get sandbox credentials at https://developer.paypal.com/dashboard/applications/sandbox")
            )
        }
        return try {
            // Clear any cached token so we do a fresh auth check
            cachedAccessToken = null
            getAccessToken()
            Result.success("Connected to PayPal sandbox")
        } catch (e: Exception) {
            Result.failure(
                Exception("Invalid credentials. Get sandbox credentials at https://developer.paypal.com/dashboard/applications/sandbox")
            )
        }
    }

    /** Clear cached token (e.g., on 401 errors). */
    fun clearAccessToken() {
        cachedAccessToken = null
    }

    private fun readResponse(connection: HttpURLConnection): String {
        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }
        val reader = BufferedReader(InputStreamReader(stream))
        val response = reader.readText()
        reader.close()
        return response
    }
}

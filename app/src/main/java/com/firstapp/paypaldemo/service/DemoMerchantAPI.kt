package com.firstapp.paypaldemo.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * A simple demo merchant server API class:
 *  - Creates orders on a sample merchant server
 *  - Completes or captures orders
 *  - Optionally fetches a client ID (if your server returns it)
 *
 * NOTE: This is purely an example. Adapt it to match your real merchant server's endpoints.
 */
object DemoMerchantAPI {

    // Example base URL to your Heroku or any server that can create orders
    private const val BASE_URL = "https://ppcp-mobile-demo-sandbox-87bbd7f0a27f.herokuapp.com"

    /**
     * Example function to fetch a client ID from your server's `/client_id` endpoint.
     * If you don't need dynamic client IDs, you can skip this.
     */
    suspend fun getClientID(): String {
        val endpoint = "$BASE_URL/client_id"
        val responseData = makeHttpRequest(urlString = endpoint, method = "GET")
        val json = JSONObject(responseData)
        return json.getString("clientID") // depends on your server’s JSON structure
    }

    /**
     * Example function to create an order on the merchant server.
     */
    suspend fun createOrder(
        intent: String,
        purchaseUnits: List<PurchaseUnit>
    ): Order {
        val endpoint = "$BASE_URL/orders"

        val purchaseUnitsArray = JSONArray()
        purchaseUnits.forEach { pUnit ->
            purchaseUnitsArray.put(pUnit.toJson())
        }

        // Build JSON payload
        val bodyJson = JSONObject().apply {
            put("intent", intent)
            put("purchaseUnits", purchaseUnitsArray)
        }
        println("createOrder JSON: $bodyJson")
        // Send POST request
        val responseData = makeHttpRequest(
            urlString = endpoint,
            method = "POST",
            jsonBody = bodyJson.toString()
        )

        // Parse JSON into an Order
        return parseOrder(responseData)
    }

    /**
     * Example function to complete an order on the merchant server.
     * If your server uses separate endpoints for capture vs. authorize,
     * you can adapt accordingly.
     */
    suspend fun completeOrder(
        orderID: String,
        intent: String // "CAPTURE" or "AUTHORIZE"
    ): Order {

        val endpoint = "$BASE_URL/orders/$orderID/${intent.lowercase()}"

        val responseData = makeHttpRequest(
            urlString = endpoint,
            method = "POST"
        )
        return parseOrder(responseData)
    }

    /**
     * A minimal example HTTP helper using HttpURLConnection.
     *
     * @param urlString The endpoint URL.
     * @param method "GET", "POST", etc.
     * @param jsonBody Optional JSON body for POST/PUT calls.
     *
     * @return The raw response body as a String.
     */
    private suspend fun makeHttpRequest(
        urlString: String,
        method: String,
        jsonBody: String? = null
    ): String = withContext(Dispatchers.IO) {
        val url = URL(urlString)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Content-Type", "application/json")
            // for POST requests only
            doOutput = true
        }

        // If we have a JSON body, write it to output
        if (jsonBody != null && method != "GET") {
            conn.doOutput = true
            conn.outputStream.use { os ->
                os.write(jsonBody.toByteArray(Charsets.UTF_8))
            }
        }

        // Read the response
        val responseCode = conn.responseCode
        val inputStream = if (responseCode in 200..299) {
            conn.inputStream
        } else {
            conn.errorStream
        }
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val response = buildString {
            reader.forEachLine { append(it) }
        }
        conn.disconnect()

        if (responseCode !in 200..299) {
            throw RuntimeException("Server error. HTTP status: $responseCode - $response")
        }
        return@withContext response
    }

    /**
     * Parses an Order object from a JSON string
     */
    private fun parseOrder(jsonString: String): Order {
        val json = JSONObject(jsonString)
        // Adjust field names to match your server's JSON structure.
        val id = json.optString("id", "")
        val status = json.optString("status", "")

        return Order(id = id, status = status)
    }
}

/**
 * Minimal data class representing an "Order" from your server
 */
data class Order(
    val id: String,
    val status: String
)

/**
 * Minimal data class representing a "purchase unit" for createOrder
 */
data class PurchaseUnit(
    val amount: Amount
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("amount", amount.toJson())
        }
    }
}

/**
 * Minimal data class for the amount in a purchase unit
 */
data class Amount(
    val currencyCode: String,
    val value: String
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("currencyCode", currencyCode)
            put("value", value)
        }
    }
}
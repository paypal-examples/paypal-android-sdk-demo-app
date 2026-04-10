package com.example.appswitch

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent

/**
 * AppSwitchHandler - Handles PayPal app detection, redirect, and return URL parsing.
 *
 * Key behaviors from the doc:
 * - Check PayPal app installed via PackageManager (requires <queries> in manifest)
 * - Check App Links support via hasEnabledSupportedLinks()
 * - Only pass app_switch_context when BOTH checks return true
 * - Open payer-action URL with Intent.ACTION_VIEW + setPackage for app switch
 * - Fall back to Custom Tabs when PayPal app is not available
 * - Parse return URL for token/PayerID (orders) or approval_session_id (vault)
 *
 * Source: https://developer.paypal.com/docs/business/payments/app-switch/direct-api
 */
object AppSwitchHandler {

    private const val TAG = "AppSwitchHandler"
    private const val PAYPAL_PACKAGE = "com.paypal.android.p2pmobile"

    /**
     * Check if the PayPal app is installed on this device.
     * Required before passing app_switch_context in the API request.
     *
     * Note: Requires <queries><package android:name="com.paypal.android.p2pmobile" /></queries>
     * in AndroidManifest.xml (Android 11+ package visibility).
     *
     * From doc: "Note: This overload is deprecated on API 33+.
     * See PackageManager.PackageInfoFlags for the modern alternative."
     */
    fun isPayPalAppInstalled(activity: Activity): Boolean {
        return try {
            activity.packageManager.getPackageInfo(PAYPAL_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Check if the device supports App Links for the PayPal app.
     *
     * From doc: resolves https://www.paypal.com and checks if the PayPal app
     * is the default handler. If not, App Links are not verified and the
     * system may show a chooser dialog instead of opening PayPal directly.
     */
    fun hasEnabledSupportedLinks(activity: Activity): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://www.paypal.com")
        }
        val resolveInfo = activity.packageManager.resolveActivity(
            intent, PackageManager.MATCH_DEFAULT_ONLY
        )
        return resolveInfo?.activityInfo?.packageName == PAYPAL_PACKAGE
    }

    /**
     * Determine if app switch should be used.
     *
     * From doc: "Only pass app_switch_context when isPayPalAppInstalled() returns
     * true AND hasEnabledSupportedLinks() returns true."
     */
    fun shouldUseAppSwitch(activity: Activity): Boolean {
        val installed = isPayPalAppInstalled(activity)
        val linksEnabled = hasEnabledSupportedLinks(activity)
        Log.d(TAG, "PayPal installed=$installed, appLinksEnabled=$linksEnabled")
        return installed && linksEnabled
    }

    /**
     * Step 3: Redirect the buyer to PayPal.
     *
     * From doc:
     * - If app switch eligible AND hasEnabledSupportedLinks: open with Intent.ACTION_VIEW
     *   and setPackage("com.paypal.android.p2pmobile") to target PayPal directly
     * - On Android 11 and below, if App Links verification fails, the system may show
     *   a chooser dialog. setPackage prevents that.
     * - Catch ActivityNotFoundException and fall back to Custom Tabs
     * - If not eligible: go straight to Custom Tabs
     */
    fun handlePayerAction(activity: Activity, payerActionUrl: String, appSwitchEligible: Boolean) {
        if (appSwitchEligible && hasEnabledSupportedLinks(activity)) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(payerActionUrl))
                intent.setPackage(PAYPAL_PACKAGE)
                activity.startActivity(intent)
                Log.d(TAG, "Opened PayPal app via app switch")
            } catch (e: ActivityNotFoundException) {
                Log.w(TAG, "PayPal app not found, falling back to Custom Tabs", e)
                openCustomTab(activity, payerActionUrl)
            }
        } else {
            Log.d(TAG, "App switch not eligible, using Custom Tabs")
            openCustomTab(activity, payerActionUrl)
        }
    }

    /**
     * Fall back to Chrome Custom Tabs when the PayPal app is not available.
     * Dependency: androidx.browser:browser:1.8.0
     */
    private fun openCustomTab(activity: Activity, url: String) {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        customTabsIntent.launchUrl(activity, Uri.parse(url))
    }

    /**
     * Extract the payer-action URL from the API response links array.
     *
     * From doc: "The payer-action link rel value differs between flows.
     * For one-time payments it is 'payer-action'. For vaulting it is 'approve'."
     */
    fun extractPayerActionUrl(links: List<Map<String, String>>, flow: String): String? {
        val rel = if (flow == "vault") "approve" else "payer-action"
        return links.firstOrNull { it["rel"] == rel }?.get("href")
    }

    /**
     * Parse return URL query parameters.
     *
     * From doc - Return URL Query Parameters:
     * - Orders flow: token (order ID) + PayerID (buyer's PayPal payer ID)
     * - Vault flow: approval_session_id (or token_id)
     *
     * @return A sealed result indicating which flow returned and the parsed parameters.
     */
    fun parseReturnUrl(uri: Uri): ReturnResult {
        val token = uri.getQueryParameter("token")
        val payerID = uri.getQueryParameter("PayerID")

        if (token != null && payerID != null) {
            return ReturnResult.OrderApproved(orderId = token, payerId = payerID)
        }

        val approvalSessionId = uri.getQueryParameter("approval_session_id")
            ?: uri.getQueryParameter("token_id")
        if (approvalSessionId != null) {
            return ReturnResult.VaultApproved(setupTokenId = approvalSessionId)
        }

        return ReturnResult.Unknown(uri.toString())
    }

    sealed class ReturnResult {
        data class OrderApproved(val orderId: String, val payerId: String) : ReturnResult()
        data class VaultApproved(val setupTokenId: String) : ReturnResult()
        data class Unknown(val rawUrl: String) : ReturnResult()
    }
}

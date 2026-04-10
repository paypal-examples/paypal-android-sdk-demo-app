# PayPal App Switch - Android Sample App (Direct API)

Minimal Android Kotlin app for testing the PayPal App Switch Direct API integration. Built entirely from the [Direct API documentation](https://developer.paypal.com/docs/business/payments/app-switch/direct-api) with no PayPal SDK dependencies.

## Project Structure

```
app/src/main/java/com/example/appswitch/
  MainActivity.kt      - UI with Pay and Vault buttons, return handling
  PayPalService.kt      - Server-side API calls (OAuth, Orders, Vault)
  AppSwitchHandler.kt   - PayPal app detection, redirect, URL parsing

app/src/main/
  AndroidManifest.xml   - Package visibility, App Links, launchMode
  res/layout/activity_main.xml - Simple button + status layout
```

## How to Open and Run

1. Open Android Studio (Hedgehog 2023.1+ recommended)
2. File > Open > select this `android/` directory
3. Wait for Gradle sync to complete
4. Connect a physical Android device (emulators will not have the PayPal app)
5. Run the app (green play button or Shift+F10)

**Before running**, update `RETURN_DOMAIN` in `PayPalService.kt` to a domain you control, and configure the matching App Links intent filter in `AndroidManifest.xml`. For sandbox testing without a real domain, the app will fall back to Custom Tabs.

## What to Test

### Happy Path: One-Time Payment
1. Tap **Pay $25.00**
2. App creates an order via Orders v2 with `app_switch_context`
3. If PayPal app is installed and App Links verified: PayPal app opens
4. If not: Chrome Custom Tab opens PayPal checkout
5. Buyer approves payment in PayPal
6. PayPal redirects back to your app with `token` + `PayerID`
7. App captures the order and shows Order ID + Capture ID

### Happy Path: Vault (Save PayPal)
1. Tap **Save PayPal**
2. App creates a setup token via Vault v3 with `app_switch_context`
3. PayPal app or Custom Tab opens for buyer approval
4. PayPal redirects back with `approval_session_id`
5. App creates a payment token and shows the vault ID
6. The vault ID can be used for future charges without re-authentication

### Fallback Path (Custom Tabs)
1. Uninstall the PayPal app from the test device
2. Tap **Pay $25.00** or **Save PayPal**
3. Chrome Custom Tab should open instead of the PayPal app
4. Complete the flow in the browser
5. Buyer lands on `return_url` (web fallback) instead of `return_app_url`

### Error Cases
- Network errors: Disable WiFi and tap a button - should show error message
- Cancel flow: Start a payment, then press back in PayPal - no capture occurs
- Missing PayPal app: Verify Custom Tabs fallback works correctly

## Expected Results

### Successful Payment
```
--- Payment Result ---
Order ID: 5O190127TN364715T
Capture ID: 8MC585209K746631H
Status: COMPLETED
```

### Successful Vault
```
--- Vault Result ---
Setup Token: 7XS12345AB678901C
Payment Token (vault_id): 9PT98765ZY432109X
Status: VAULTED
```

### app_switch_eligibility
The API response includes `payment_source.paypal.app_switch_eligibility`. If `false`, the merchant account is not ramped for App Switch - contact your PayPal account team.

## Sandbox Credentials

The app uses hardcoded sandbox credentials in `PayPalService.kt`. These are for testing only and must be replaced with production credentials before any real deployment.

- Sandbox SMS one-time codes: `111111` or `222222`
- Create test buyer accounts at: https://developer.paypal.com/dashboard/accounts

## Doc Gaps Found

The following gaps or ambiguities were identified in the Direct API documentation during the creation of this sample app:

1. **No Android-specific vault return parameter documented.** The doc's Android `handlePayPalReturn` code checks for `approval_session_id` and `token_id` as vault return params, but the Step 4 "Return URL Query Parameters" table only lists `token` and `PayerID`. It does not mention `approval_session_id` as a return parameter, which could confuse developers who only read the table and skip the code samples.

2. **os_version not shown in minimal (App Switch tab) examples.** The minimal "App Switch" tab curl examples omit `os_version` while the verbose "cURL" tab examples include it. The parameter reference says it is optional (used for telemetry), but the inconsistency may cause confusion about whether it matters.

3. **No guidance on Digital Asset Links (assetlinks.json) setup.** The doc says to configure App Links and mentions the intent filter with `android:autoVerify="true"`, but provides no instructions or example for creating and hosting the `.well-known/assetlinks.json` file. iOS gets explicit `apple-app-site-association` instructions in the code comments.

4. **Capture request body ambiguity.** The capture endpoint examples show POST with no request body (Java uses `BodyPublishers.noBody()`, curl has no `-d` flag), but the Android sample must send something via HttpURLConnection to trigger POST. The doc does not clarify whether an empty body or no body is expected.

5. **No mention of `getPackageInfo` deprecation replacement.** The doc notes that `getPackageInfo(String, int)` is deprecated on API 33+ and references `PackageManager.PackageInfoFlags`, but does not provide the modern API 33+ code. Developers targeting API 33+ will need to look up the replacement themselves.

6. **create payment token endpoint does not mention `Prefer: return=representation` header.** The Orders capture examples include this header, but the Vault payment-tokens POST examples do not. It is unclear if the vault endpoint supports it or if the response shape differs without it.

7. **No error response examples.** The doc covers success responses thoroughly but provides no example of what a 422, 401, or other error response body looks like. The error handling table lists scenarios but not response shapes.

8. **chargeWithVaultId flow not fully documented.** The "Using a Vault ID for Future Charges" section shows a curl example creating an order with `vault_id`, but does not show whether a capture step is still needed afterward (the order presumably returns with status COMPLETED directly, or still needs capture). This sample assumes it needs capture.

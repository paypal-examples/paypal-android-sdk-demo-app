# PayPal Android Demo App – SDK & Payment Link Integration

This repository contains an **Jetpack Compose demo application** that simulates a real-world merchant app, featuring a shopping cart checkout process. The goal is to showcase core features of the **PayPal Android SDK** (Web Checkout and Card Payments), providing merchants with sample code and integration patterns to simplify development.

This app makes server-side PayPal API calls via a merchant server that uses the [PayPal Typescript Server SDK](https://github.com/paypal/PayPal-TypeScript-Server-SDK), which is in beta. Our developer docs show an example integration with direct PayPal server-side calls.

## 🎯 Purpose

This demo app serves as a reference for merchants, as an example integration application. By providing a practical and easy-to-follow example, we aim to make PayPal SDK integration smoother and faster for developers.

## 🚀 Demo App Features

- Checkout with PayPal
- Checkout with Cards
- Checkout with Payment Links `New`

## 🔧 Requirements

* **Android Studio** Ladybug (or newer)
* **Min SDK** 35+ (example)
* **PayPal Android SDK** (Web Payments + Card Payments)

## 🛠 Setup

1. **Clone this repository**:
   ```bash
   git clone https://github.com/paypal-examples/paypal-android-sdk-demo-app.git
   cd paypal-android-sdk-demo-app
   ```
2. **Open Project in Android Studio**
   * Open Android Studio
   * Select `File > Open` and choose this folder as the project root
   * Perform a Gradle sync
3. **Run the App**
   * Launch the app on an emulator or physical device
   * You should see a basic cart view with options for PayPal or Card checkout

## PayPal Web Checkout Flow

1. Enable the "Use PayPal SDK" segmented button option at the top of the screen
1. Press the "Pay with PayPal" button at the bottom of the screen
1. Wait for the Chrome Custom Tab to load
1. Authorize a payment method using an existing PayPal Sandbox account

When complete, the Chrome Custom Tab will redirect back into the demo app with a confirmation screen.

## Card Payment Flow

1. Enable the "Use PayPal SDK" segmented button option at the top of the screen
1. Press the "Pay with Card" button at the bottom of the screen
1. When prompted, enter credit card details
1. Press "Submit" to complete the order

When complete, the demo app will redirect to a confirmation screen.

## ⚠️ Demo Keystore

This repo inlcudes a [debug.keystore](app/keystores/debug.keystore) for signing debug versions of the demo app. This is necessary for deep linking since the `assetlinks.json` includes a `sha256` fingerprint derived from the debug signing key embedded in this repository.

In a production setting, your keystore should not be public. We are only including it in this project to allow you to sign a local build of the demo app with a sha256 fingerprint that is valid for Android app links destined for the demo server.

## 🌐 Android App Links Setup

To set up app links in your own Android app, follow the steps provided by the Android team on [developer.android.com](https://developer.android.com/training/app-links).

## 📍 Where to Find Key Business Logic

If you want to skip UI details and jump straight into the **business logic** (server calls and SDK integrations) of the demo app, here are the main files:

### PayPalViewModel.kt
* Wraps all **PayPal Web Checkout** logic:
  * Creating orders on the server (`DemoMerchantAPI`)
  * Starting the browser flow via `PayPalWebCheckoutClient`
  * Finishing checkout after the user returns from Chrome Custom Tab

### CardPaymentViewModel.kt
* Demonstrates **Card Payments**:
  * Creating orders on the server
  * Approving the order with the PayPal SDK `CardPayments` module
  * Capturing the order upon success

### CheckoutCoordinatorViewModel.kt
* High-level **coordinator** that unifies PayPal and Card checkout flows:
  * Maintains the `CheckoutState` (Idle, Loading, OrderComplete, Error)
  * Sets up the PayPal client and can orchestrate card logic

### DemoMerchantAPI.kt
* Simulated server calls (`createOrder`, `completeOrder`)
* In real apps, replace with your own backend integration

## 📂 Project Structure (UI + Flow)

### 1. MainActivity
* Creates or retrieves `CheckoutCoordinatorViewModel`
* Handles `onNewIntent` for PayPal browser switch return
* Displays the main Compose UI

### 2. CheckoutFlow (Jetpack Compose)
* A NavHost that moves between "Cart", "Card Checkout" (for card checkout flow), and "OrderComplete" destinations
* Observes the coordinator's state for loading/error messages

### 3. CartView
* Displays items in the cart and total amount
* Buttons to "Pay with PayPal" or "Pay with Card"
* Tapping either calls the coordinator to start that checkout flow

### 4. CardCheckoutView
* A **Compose** screen for users to enter card details (card number, expiration, CVV)
* References `CardPaymentViewModel` for network calls (order creation, approval, capture)
* On success, navigates to `OrderCompleteView`

### 5. OrderCompleteView
* Displays a final "Thank you" message and the captured order ID once checkout is successful

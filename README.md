# paypal-android-sdk-demo-app
This repository contains a **demo app** that simulates a real-world merchant application, featuring a store, cart, and checkout process.
The goal is to showcase core features of the **PayPal Android SDK** (Web Checkout and Card Payments), providing merchants with sample code and integration patterns to simplify development.
This app makes merchant server side PayPal API calls via [PayPal Typescript Server SDK version 0.5.1](https://github.com/paypal/PayPal-TypeScript-Server-SDK) which is in beta.
Our developer docs show an example integration with direct PayPal server-side calls.

## 🚀 Version 1.0 Features

1. **Checkout with PayPal**
   * Seamless PayPal web checkout experience via Chrome Custom Tabs

2. **Checkout with Card**
   * Collect card details in-app, then approve & capture the order via PayPal's Card Payments APIs

## 🎯 Purpose

This demo app serves as a reference for merchants, as an example integration application.
By providing a practical and easy-to-follow example, we aim to make PayPal SDK integration smoother and faster for developers.

## PayPal Web Checkout Flow

1. Tapping Pay with PayPal calls DemoMerchantAPI.createOrder
2. Opens Chrome Custom Tab to let the user complete the PayPal flow
3. On return, onNewIntent triggers finishPayPalCheckout in PayPalViewModel, which captures the order, then navigates to OrderCompleteView

## Card Payment Flow

1. Tapping Pay with Card navigates to CardCheckoutView
2. User enters card info; CardPaymentViewModel handles:
   * createOrder on server
   * approveOrder with the PayPal Card SDK
   * completeOrder on server
3. If successful, navigates to OrderCompleteView

## 📍 Where to Find Key Logic

If you want to skip UI details and jump straight to **business logic** (server calls and SDK integrations), here are the main files:

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

## 🔧 Requirements

* **Android Studio** Ladybug (or newer)
* **Min SDK** 35+ (example)
* **PayPal Android SDK** (Web Payments + Card Payments)

## 🛠 Setup

1. **Clone this repository**:
  
   * git clone https://github.com/paypal-examples/paypal-android-sdk-demo-app.git
   * cd paypal-android-sdk-demo-app
 

2. **Open in Android Studio**
   * Select File > Open, choose this folder
   * Let Gradle sync everything


3. **Run the App**
   * Use an emulator or physical device
   * You should see a basic cart view with options for PayPal or Card checkout
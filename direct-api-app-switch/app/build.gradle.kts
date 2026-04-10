import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// Load credentials from local.properties
val localProperties = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        localPropsFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.example.appswitch"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.appswitch"
        minSdk = 24        // Android 7.0 as documented
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Inject PayPal credentials into BuildConfig from local.properties
        buildConfigField("String", "PAYPAL_CLIENT_ID", "\"${localProperties.getProperty("PAYPAL_CLIENT_ID", "")}\"")
        buildConfigField("String", "PAYPAL_CLIENT_SECRET", "\"${localProperties.getProperty("PAYPAL_CLIENT_SECRET", "")}\"")
        buildConfigField("String", "RETURN_DOMAIN", "\"${localProperties.getProperty("RETURN_DOMAIN", "https://example.com")}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Custom Tabs for browser fallback (from doc: "implementation 'androidx.browser:browser:1.8.0'")
    implementation("androidx.browser:browser:1.8.0")
}

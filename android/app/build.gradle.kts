plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.gpstracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.gpstracker"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // USB 시리얼 (CDC-ACM, FTDI, CP210x, CH34x, PL2303 등 지원)
    implementation("com.hoho.android:usb-serial-for-android:3.8.1")
    // assets를 https://appassets.androidplatform.net 으로 안전하게 제공
    implementation("androidx.webkit:webkit:1.11.0")
}

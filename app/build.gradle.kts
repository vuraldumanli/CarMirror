plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.vural.carmirror"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.vural.carmirror"
        minSdk = 29
        targetSdk = 34
        versionCode = 3
        versionName = "1.2"
    }

    signingConfigs {
        // Sabit anahtar: güncellemeler eski sürümü silmeden kurulabilsin
        create("fixed") {
            storeFile = file("carmirror.jks")
            storePassword = "carmirror"
            keyAlias = "carmirror"
            keyPassword = "carmirror"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fixed")
            // Android Auto "Bilinmeyen kaynaklar" yalnızca geliştirici derlemelerini kabul ediyor olabilir
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.car.app:app:1.4.0")
    implementation("androidx.core:core-ktx:1.13.1")
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ir.prdwest.schoolservicemanager"
    compileSdk = 35

    defaultConfig {
        applicationId = "ir.prdwest.schoolservicemanager"
        minSdk = 23
        targetSdk = 35
        versionCode = 4
        versionName = "1.3.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    signingConfigs {
        create("release") {
            val ksPath = rootProject.file("release-keystore.jks")
            if (ksPath.exists()) {
                storeFile = ksPath
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "SchoolService2026"
                keyAlias = System.getenv("KEY_ALIAS") ?: "schoolservice"
                keyPassword = System.getenv("KEY_PASSWORD") ?: "SchoolService2026"
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            val ksPath = rootProject.file("release-keystore.jks")
            if (ksPath.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

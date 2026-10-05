plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android { namespace="ir.prdwest.schoolservicemanager"; compileSdk=35
 defaultConfig { applicationId="ir.prdwest.schoolservicemanager"; minSdk=23; targetSdk=35; versionCode=1; versionName="1.0.0" }
 compileOptions {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
 }
 kotlinOptions { jvmTarget = "17" }
}

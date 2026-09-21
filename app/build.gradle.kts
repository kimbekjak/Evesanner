plugins { id("com.android.application") }

android {
    namespace = "com.kyunghoon.eversolocoverscanner"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kyunghoon.eversolocoverscanner"
        minSdk = 23
        targetSdk = 28
        versionCode = 1
        versionName = "0.1"
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
}

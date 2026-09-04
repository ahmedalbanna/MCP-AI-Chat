plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(11)
}

android {
    namespace = "com.aistudio.mcpchat.shared"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Upload-key credentials are optional for local/CI builds and never committed to source.
// Release AABs are only Play-upload-ready when all values are supplied securely.
val keystorePath = providers.environmentVariable("FOLQIO_KEYSTORE_PATH").orNull
val keystorePassword = providers.environmentVariable("FOLQIO_KEYSTORE_PASSWORD").orNull
val uploadKeyAlias = providers.environmentVariable("FOLQIO_KEY_ALIAS").orNull
val uploadKeyPassword = providers.environmentVariable("FOLQIO_KEY_PASSWORD").orNull
val uploadKeyAvailable = listOf(
    keystorePath, keystorePassword, uploadKeyAlias, uploadKeyPassword
).all { !it.isNullOrBlank() }

android {
    signingConfigs {
        if (uploadKeyAvailable) {
            create("upload") {
                storeFile = file(requireNotNull(keystorePath))
                storePassword = requireNotNull(keystorePassword)
                keyAlias = requireNotNull(uploadKeyAlias)
                keyPassword = requireNotNull(uploadKeyPassword)
            }
        }
    }

    namespace = "com.brendigo.folqio"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.brendigo.folqio"
        minSdk = 26
        targetSdk = 36
        versionCode = 15
        versionName = "0.1.14"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            if (uploadKeyAvailable) {
                signingConfig = signingConfigs.getByName("upload")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}

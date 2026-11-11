plugins {
    id("com.android.application")
}

android {
    namespace = "com.pilotcompanion.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pilotcompanion.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 12
        versionName = "0.12.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Signing material is supplied only by GitHub Actions secrets or local environment.
    // Never commit a keystore or passwords to this repository.
    val signingPath = System.getenv("PILOT_SIGNING_STORE_FILE")
    val signingPassword = System.getenv("PILOT_SIGNING_STORE_PASSWORD")
    val signingAlias = System.getenv("PILOT_SIGNING_KEY_ALIAS")
    val signingKeyPassword = System.getenv("PILOT_SIGNING_KEY_PASSWORD")
    val hasReleaseSigning = listOf(signingPath, signingPassword, signingAlias, signingKeyPassword)
        .all { !it.isNullOrBlank() }
    if (hasReleaseSigning) {
        signingConfigs {
            create("pilotRelease") {
                storeFile = file(signingPath!!)
                storePassword = signingPassword
                keyAlias = signingAlias
                keyPassword = signingKeyPassword
            }
        }
    }
    buildTypes {
        release {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("pilotRelease")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("com.google.mlkit:text-recognition:16.0.1")
    testImplementation("junit:junit:4.13.2")
}

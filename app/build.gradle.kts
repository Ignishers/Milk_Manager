plugins {
    alias(libs.plugins.android.application)
}

// Set Base Name for output files (e.g. MilkManager2-release.apk)
setProperty("archivesBaseName", "MilkManager2")

android {
    namespace = "com.ignishers.milkmanager2"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ignishers.milkmanager2"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    
    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
        }
    }

    lint {
        abortOnError = false
    }
    
    // Use androidComponents block for modern customization if needed
    androidComponents {
        // onVariants { ... }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.legacy.support.v4)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.fragment)
    implementation(libs.mpandroidchart)

    // Google Drive Backup
    implementation(libs.google.play.auth)
    implementation(libs.google.drive)
    implementation(libs.google.api.client.android)
    implementation(libs.google.http.client.gson)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

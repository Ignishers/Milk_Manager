import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}
val supabaseUrl = localProperties.getProperty("SUPABASE_URL", "") as String? ?: ""
val supabaseKey = localProperties.getProperty("SUPABASE_KEY", "") as String? ?: ""

// Set Base Name for output files (e.g. MilkManager2-release.apk)
base {
    archivesName.set("MilkManager2")
}

android {
    namespace = "com.ignishers.milkmanager2"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ignishers.milkmanager2"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "2.4.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", "\"${supabaseUrl}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${supabaseKey}\"")
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

    // Networking & Sync
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.jbcrypt)
    implementation(libs.work.runtime)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

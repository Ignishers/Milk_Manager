plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.ignishers.milkmanager2"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.ignishers.milkmanager2"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.5"

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
    }

    applicationVariants.all {
        outputs.all {
            val output = this as? com.android.build.gradle.api.ApkVariantOutput
            if (output != null && buildType.name == "release") {
                output.outputFileName = "MilkManager2.apk"
            }
        }
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}
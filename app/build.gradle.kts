plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.seg2105_project_1_tutor_registration_form"

    // Needed because androidx.activity:activity:1.11.0 requires API 36+
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.seg2105_project_1_tutor_registration_form"
        minSdk = 24
        targetSdk = 36   // OK to keep 35, but 36 is fine too
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_11   // or 17 if your AGP needs it
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)          // this can stay at 1.11.0 once compileSdk=36
    implementation(libs.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

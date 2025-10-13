plugins {
    alias(libs.plugins.android.application)
<<<<<<< HEAD

    // Option A: just apply it here (version is declared in settings.gradle.kts)
=======
>>>>>>> origin/main
    id("com.google.gms.google-services")
}

android {
<<<<<<< HEAD
    namespace = "com.example.seg2105_project_1_tutor_registration_form"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.seg2105_project_1_tutor_registration_form"
=======
    namespace = "ca.SEG2105group22.otams"
    compileSdk = 36

    defaultConfig {
        applicationId = "ca.SEG2105group22.otams"
>>>>>>> origin/main
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
<<<<<<< HEAD
=======

>>>>>>> origin/main
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
<<<<<<< HEAD

=======
>>>>>>> origin/main
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
<<<<<<< HEAD

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // existing deps
=======
}

dependencies {

>>>>>>> origin/main
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
<<<<<<< HEAD

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Firebase (use BoM to align versions)
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
}

=======
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
}
>>>>>>> origin/main

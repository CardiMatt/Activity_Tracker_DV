plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services") // Plugin Google
    id("com.google.dagger.hilt.android")  // Hilt per DI
    id("com.google.devtools.ksp") version "1.9.0-1.0.13"  // KSP
}

android {
    namespace = "com.example.activity_tracker_dv"
    compileSdk = 34

    packaging {
        resources {
            excludes += "META-INF/gradle/incremental.annotation.processors"
        }
    }

    defaultConfig {
        applicationId = "com.example.activity_tracker_dv"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation(libs.firebase.database.ktx)
    implementation(libs.androidx.room.common)
    implementation(libs.androidx.room.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Firebase BOM
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Hilt dependencies
    implementation(libs.hilt.android)
    ksp(libs.androidx.room.compiler)  // Usa KSP per Hilt

    // Room dependencies
    implementation(libs.hilt.android.compiler)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)  // Usa KSP per Room
    ksp(libs.hilt.android.compiler)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")  // Salva gli schemi di Room
}

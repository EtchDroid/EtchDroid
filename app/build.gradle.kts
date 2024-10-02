@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("de.mannodermaus.android-junit5") version "1.10.0.0"
}

android {
    namespace = "eu.depau.etchdroid"
    compileSdk = 35

    defaultConfig {
        applicationId = "eu.depau.etchdroid"
        minSdk = 21
        targetSdk = 35
        versionCode = 22
        versionName = "1.9.rc2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro",
            )
        }
    }
    flavorDimensions += "store"
    productFlavors {
        create("foss") {
            isDefault = true
            dimension = "store"
        }
        create("gplay") {
            dimension = "store"
        }
    }
    packaging {
        resources {
            excludes += "META-INF/AL2.0"
            excludes += "META-INF/LGPL2.1"
            excludes += "META-INF/licenses/ASM"
            excludes += "META-INF/libaums_release.kotlin_module"
            excludes += "win32-x86/attach_hotspot_windows.dll"
            excludes += "win32-x86-64/attach_hotspot_windows.dll"
        }
    }
    compileOptions {
        encoding = "UTF-8"
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = rootProject.extra["composeVersion"] as String
    }
    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
            it.maxHeapSize = "4g"
        }
    }
}

dependencies {
    val composeVersion = rootProject.extra["composeVersion"] as String
    val composeBomVersion = rootProject.extra["composeBomVersion"] as String
    val kotlinVersion = rootProject.extra["kotlinVersion"] as String

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-service:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")

    // Compose
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui:${composeVersion}")
    implementation("androidx.compose.ui:ui-tooling-preview:${composeVersion}")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("com.google.accompanist:accompanist-navigation-animation:0.32.0")
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.3")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")
    implementation("com.airbnb.android:lottie-compose:6.2.0")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("io.coil-kt:coil-gif:2.5.0")
    implementation("androidx.compose.ui:ui-graphics")
    implementation(platform("androidx.compose:compose-bom:${composeBomVersion}"))

    // Core dependencies
    implementation("me.jahnen.libaums:core:0.10.0")
    implementation("me.jahnen.libaums:libusbcommunication:0.3.0")
    implementation(platform("androidx.compose:compose-bom:${composeBomVersion}"))
    androidTestImplementation(platform("androidx.compose:compose-bom:${composeBomVersion}"))

    // Google Play
    "gplayImplementation"("com.google.android.play:review:2.0.1")
    "gplayImplementation"("com.google.android.play:review-ktx:2.0.1")

    // Test dependencies
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.mockito:mockito-core:5.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.7.3")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.3")
    androidTestImplementation(platform("androidx.compose:compose-bom:${composeBomVersion}"))
    debugImplementation("androidx.compose.ui:ui-tooling:${composeVersion}")
    debugImplementation("androidx.compose.ui:ui-test-manifest:${composeVersion}")
}
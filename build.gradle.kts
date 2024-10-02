buildscript {
    val composeVersion by extra("1.5.15")
    val composeBomVersion by extra("2024.09.03")
    val kotlinVersion by extra("1.9.25")  // Also update the Gradle plugin version below!
}
plugins {
    id("com.android.application") version "8.7.0" apply false
    id("com.android.library") version "8.7.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.25" apply false
}

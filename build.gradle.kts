plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.services) apply false
    // id("androidx.navigation.safeargs.kotlin") version "2.7.7" apply false  // TODO: Enable when Java 17 is available
}

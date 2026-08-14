plugins {
    id("com.android.application") version "9.2.1" apply false
    // AGP 9 has built-in Kotlin support — org.jetbrains.kotlin.android must NOT be
    // applied here or the build fails with a duplicate-plugin error. Compose still
    // needs its own compiler plugin:
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20" apply false
}

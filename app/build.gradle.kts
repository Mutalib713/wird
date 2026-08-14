plugins {
    id("com.android.application") // AGP 9: Kotlin support is built in
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.mosman.wird"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mosman.wird"
        // API 26 is the floor. Two reasons, both deliberate:
        //   1. java.time.LocalDate is available natively from 26, so the day/streak
        //      maths needs no desugaring.
        //   2. Testers are on Tecno / Infinix / itel. 26 reaches phones sold from
        //      2017 onward, which is well below anything they are carrying.
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Debug key for now so a release build installs over a debug one. A real
            // keystore comes with the first tester distribution (PLAN task 16).
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        // A lint warning that nobody fixes is noise. check.ps1 must mean something, so
        // warnings are errors by default.
        warningsAsErrors = true
        abortOnError = true

        // ...but only for things that are about OUR code. These three are about the
        // outside world moving, and a check that fails on someone else's release
        // schedule is a check that gets ignored — which is worse than no check.
        disable += setOf(
            // "A newer version of X is available." Fires whenever any dependency ships
            // a release. Upgrades are a deliberate task, not a build failure.
            "GradleDependency",
            // "compileSdk 37 is available." Only android-36 and android-36.1 are
            // installed on this machine, and 36 is what Thrum builds against. Revisit
            // before the first public build, not on a random Tuesday.
            "OldTargetApi",
            // No app icon yet, on purpose. The icon comes out of the design-studio
            // pass, where the palette is Mutalib's to choose — picking a placeholder
            // here is how a default quietly becomes the brand. Tracked in PLAN task 16.
            "MissingApplicationIcon",
        )
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui:1.9.0")
    implementation("androidx.compose.foundation:foundation:1.9.0")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.9.0")
    // Debug only — the preview renderer is a build-time tool, not something users ship.
    debugImplementation("androidx.compose.ui:ui-tooling:1.9.0")

    // The whole domain layer is pure Kotlin with no Android dependency, so junit
    // alone tests all of it.
    testImplementation("junit:junit:4.13.2")
}

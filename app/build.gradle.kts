plugins {
    id("com.android.application") // AGP 9: Kotlin support is built in
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.mosman.wird"
    compileSdk = 36

    /**
     * The NDK that builds whisper.cpp. PLAN task 14.
     *
     * ⚠ Pinned to the version actually on this machine, and it is **r27b** rather than the
     * r27 that `sdkmanager` was asked for - four sdkmanager attempts failed on this
     * connection and the zip was fetched directly instead. PROFILE.md § 5aw carries the
     * curl command, because the next machine will need it too.
     */
    ndkVersion = "27.1.12297006"

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    defaultConfig {
        applicationId = "com.mosman.wird"
        // API 26 is the floor. Two reasons, both deliberate:
        //   1. java.time.LocalDate is available natively from 26, so the day/streak
        //      maths needs no desugaring.
        //   2. Testers are on Tecno / Infinix / itel. 26 reaches phones sold from
        //      2017 onward, which is well below anything they are carrying.
        minSdk = 26

        // ⚠ **arm64 only, and that is a real exclusion rather than an oversight.** Every
        // native ABI multiplies the size of the shipped library, and § 10 measures this app
        // in kilobytes. arm64-v8a covers effectively every Android phone sold in years,
        // including the Transsion devices the testers carry - but a genuinely old 32-bit
        // handset gets no recitation check at all. WhisperLib.available answers false there
        // and nothing else breaks, which is the behaviour an optional feature should have.
        ndk {
            abiFilters += "arm64-v8a"
        }

        externalNativeBuild {
            cmake {
                // Upstream's own flags for the Android build. -DNDEBUG drops ggml's asserts,
                // which are hot-path checks in a library doing matrix maths on a phone.
                cppFlags += listOf("-O3", "-DNDEBUG")
                arguments += listOf("-DANDROID_STL=c++_shared")
            }
        }
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        debug {
            /**
             * ⚠ **x86_64 exists so the recitation check can be tested at all.**
             *
             * The emulator is `sdk_gphone64_x86_64`. An arm64-only build loads no whisper
             * library there, so `WhisperLib.available` answers false and the entire feature is
             * untestable anywhere except Mutalib's own phone — which § 10's device rules say is
             * his daily handset, not a test rig.
             *
             * It costs about 4 MB on a debug APK nobody ships. **Release stays arm64 only.**
             */
            ndk {
                abiFilters += "x86_64"
            }
        }

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
            // "A newer version of Gradle than X is available." Same outside-world schedule.
            "AndroidGradlePluginVersion",
            // "compileSdk 37 is available." Only android-36 and android-36.1 are
            // installed on this machine, and 36 is what Thrum builds against. Revisit
            // before the first public build, not on a random Tuesday.
            "OldTargetApi",
            // ⚠ **The fourth disable, and the rule says write down why.**
            //
            // "Missing x86_64 ABI support for ChromeOS." Wird ships arm64-v8a because that is
            // every Android phone sold in years, including the Transsion handsets the testers
            // carry, and each extra ABI adds roughly 4 MB of whisper.cpp to an APK § 10
            // measures in kilobytes. **Nobody reads their daily wird on a Chromebook**, and
            // ChromeOS runs arm64 through its own binary translator anyway.
            //
            // The check is right in general and wrong for this app, which is exactly the kind
            // of disable that needs a sentence rather than a silent entry. Debug builds *do*
            // add x86_64 — see buildTypes — because the emulator is x86_64 and the recitation
            // check would otherwise be untestable anywhere but his own phone. Lint reads the
            // literal defaultConfig line rather than the merged variant, so it flags it anyway.
            "ChromeOsAbiSupport",
            // ⚠ **The fifth disable: AAPT2 vs Lint on adaptive icon folder qualifier.**
            //
            // AAPT2 requires `mipmap-anydpi-v26` so that adaptive XML icons take precedence
            // over density-specific raster drawables (e.g. `mipmap-xxhdpi/ic_launcher.png`).
            // When named `mipmap-anydpi` without `-v26`, AAPT2 selects raster PNGs instead,
            // which causes Pixel Launcher to wrap the legacy icon inside an unwanted white circular plate.
            // Lint flags `-v26` under ObsoleteSdkInt because minSdk is 26, but removing it breaks
            // the launcher icon on real devices.
            "ObsoleteSdkInt",
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
    // Icons for the chrome bar. The *core* set only — `material-icons-extended` is a
    // several-megabyte library and we need three glyphs.
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    // **The home-screen widget (task 10).** Glance is Compose's dialect for app widgets:
    // you write composables, it translates them into the RemoteViews the launcher can host.
    // Writing RemoteViews by hand is the alternative and it is XML plus imperative setters.
    //
    // **Measured, per § 10, rather than assumed: 264 KB.** The debug APK went 17.27 MB ->
    // 17.52 MB, taken by building the same tree with and without this. That is far less than
    // "it ships its own runtime" suggests, and less than a single mushaf page's audio at
    // 128 kbps - and it is the debug build, so R8 has not run on it yet.
    implementation("androidx.glance:glance-appwidget:1.1.1")

    // Debug only — the preview renderer is a build-time tool, not something users ship.
    debugImplementation("androidx.compose.ui:ui-tooling:1.9.0")

    // The whole domain layer is pure Kotlin with no Android dependency, so junit
    // alone tests all of it.
    testImplementation("junit:junit:4.13.2")

    // **Test-only, and it buys a real capability rather than convenience.**
    //
    // `org.json` ships with Android as an API but as a *stub* on the JVM unit-test
    // classpath — every method throws `Method ... not mocked`. That silently makes the two
    // classes holding this app's actual records, `DayLogStore` and `ConversationStore`,
    // untestable without an emulator. This is the real implementation, so they can be.
    //
    // The alternative Android suggests, `unitTests.isReturnDefaultValues = true`, makes the
    // stubs return null instead of throwing — which turns "this code never ran" into a
    // passing test. That is worse than no test.
    //
    // Nothing here reaches the APK: `testImplementation` is compile-and-run for unit tests
    // only, so app size is untouched. § 10.
    testImplementation("org.json:json:20250107")
}

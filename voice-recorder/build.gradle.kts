plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    androidLibrary {
        namespace = "com.rapido.voice_recorder"
        compileSdk = 35
        minSdk = 24

//        withHostTestBuilder {
//        }

//        withDeviceTestBuilder {
//            sourceSetTreeName = "test"
//        }.configure {
//            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//        }
    }

    val xcfName = "voice-recorderKit"

    iosSimulatorArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    // Source set declarations.
    // Declaring a target automatically creates a source set with the same name. By default, the
    // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
    // common to share sources between related targets.
    // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime.v060)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                // Add Android-specific dependencies here. Note that this source set depends on
                // commonMain by default and will correctly pull the Android artifacts of any KMP
                // dependencies declared in commonMain.
            }
        }

//        getByName("androidDeviceTest") {
//            dependencies {
//                implementation(libs.androidx.runner)
//                implementation(libs.androidx.core.ktx)
//                implementation(libs.androidx.junit)
//            }
//        }

        iosMain {
            dependencies {
                // Add iOS-specific dependencies here. This a source set created by Kotlin Gradle
                // Plugin (KGP) that each specific iOS target (e.g., iosX64) depends on as
                // part of KMP's default source set hierarchy. Note that this source set depends
                // on common by default and will correctly pull the iOS artifacts of any
                // KMP dependencies declared in commonMain.
            }
        }
    }

}
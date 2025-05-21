plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidLibrary {
        namespace = "com.rapido.chat"
        compileSdk = 35
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = "chatKit"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":voice-recorder"))
                implementation(libs.kotlinx.datetime.v060)
                implementation(libs.kotlinx.coroutines.core)

                // JetBrains Compose Multiplatform dependencies
                api(compose.runtime)
                api(compose.foundation)
                api(compose.ui)
                api(compose.material3)
                implementation(libs.compose.material.icons.extended)

                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                api(compose.components.resources)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        val androidMain by getting {
            dependencies {
                // Android-specific Compose dependencies if needed
                // For example, if you need Android-specific compose extensions:
                // implementation(libs.androidx.activity.compose)
            }
        }

        val iosSimulatorArm64Main by getting

        val iosMain by creating {
            dependsOn(commonMain)
            iosSimulatorArm64Main.dependsOn(this)
        }

        val iosSimulatorArm64Test by getting

        val iosTest by creating {
            dependsOn(commonTest)
            iosSimulatorArm64Test.dependsOn(this)
        }
    }
}


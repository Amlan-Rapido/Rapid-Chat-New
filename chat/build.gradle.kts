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
            isStatic = true
            // Export dependencies that are used in public API
            export(libs.koin.core)
            export(libs.koin.compose)
            export(libs.compose.runtime)
            export(libs.compose.foundation)
            export(libs.compose.material3)
            export(libs.compose.components.resources)
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":voicemessagesdk"))
                implementation(libs.kotlinx.datetime.v060)
                implementation(libs.kotlinx.coroutines.core)

                // JetBrains Compose Multiplatform dependencies
                api(libs.compose.runtime)
                api(libs.compose.foundation)
                api(libs.compose.ui)
                api(libs.compose.material3)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.koin.core)
                implementation(libs.koin.compose)

                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                api(libs.compose.components.resources)
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
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(libs.compose.components.resources)
                api(libs.koin.core)
                api(libs.koin.compose)
            }
        }

        val iosSimulatorArm64Test by getting
        val iosTest by creating {
            dependsOn(commonTest)
            iosSimulatorArm64Test.dependsOn(this)
        }
    }
}


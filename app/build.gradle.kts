import org.jetbrains.kotlin.kapt3.base.Kapt.kapt

plugins {
    id("com.android.application") // or id("com.android.library")
    kotlin("multiplatform")
}

kotlin {
    android()

    val coroutinesVersion = "1.6.4"
    val kableVersion = "0.19.0"

    sourceSets {
        val commonMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${coroutinesVersion}")
                implementation("com.juul.kable:core:${kableVersion}")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${coroutinesVersion}")
            }
        }
    }

}

android {
    compileSdk = 32
    namespace = "com.example.mysquaterkennung"

    defaultConfig {
        applicationId = "com.example.kablelibtest"
        minSdk = 26
        targetSdk = 32
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation("com.google.code.gson:gson:2.8.8")
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("com.google.android.material:material:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.1")
}
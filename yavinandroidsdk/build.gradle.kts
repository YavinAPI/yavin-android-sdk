plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("kotlinx-serialization")
    id("dagger.hilt.android.plugin")
    kotlin("kapt")
    id("maven-publish")
}

android {
    namespace = "com.yavin.yavinandroidsdk"
    compileSdk = 33

    defaultConfig {
        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.navigation:navigation-runtime-ktx:2.5.2")
    implementation("com.google.android.material:material:1.7.0")

    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

    implementation("androidx.work:work-runtime-ktx:2.8.1")

    implementation("io.getstream:stream-log-android:1.1.4")
    implementation("io.getstream:stream-log-android-file:1.1.4")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.44")
    kapt("com.google.dagger:hilt-android-compiler:2.44")
    implementation("androidx.hilt:hilt-work:1.0.0")
    kapt("androidx.hilt:hilt-compiler:1.0.0")

    val coroutinesVersion = "1.6.0"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.4")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.0")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.YavinAPI"
            artifactId = "yavin-android-sdk"
            version = "3.0.0-SNAPSHOT-v2"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
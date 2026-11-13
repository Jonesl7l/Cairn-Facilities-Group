import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(buildLibs.plugins.android.application)
    alias(buildLibs.plugins.kotlin.compose)
    alias(buildLibs.plugins.kotlin.serialization)
    alias(buildLibs.plugins.ksp)
    alias(buildLibs.plugins.hilt)
    alias(buildLibs.plugins.room3)
}

android {
    namespace = "com.jonesl7l.carinfg"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.jonesl7l.carinfg"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "DEFAULT_BASE_URL", "\"http://10.0.2.2:3000/\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            all {
                it.jvmArgs("-XX:+EnableDynamicAgentLoading")
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.exifinterface)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.bundles.navigation)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.bundles.room)
    ksp(libs.androidx.room3.compiler)
    implementation(libs.bundles.paging)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.work.runtime)

    implementation(libs.bundles.network)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.bundles.coil)
    implementation(libs.play.services.location)

    testImplementation(testLibs.bundles.unit.test)

    debugImplementation(testLibs.androidx.compose.ui.test.manifest)
    androidTestImplementation(testLibs.bundles.android.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    kspAndroidTest(libs.hilt.compiler)
}

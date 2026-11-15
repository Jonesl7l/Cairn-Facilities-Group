import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(buildLibs.plugins.android.application)
    alias(buildLibs.plugins.kotlin.compose)
    alias(buildLibs.plugins.kotlin.serialization)
    alias(buildLibs.plugins.ksp)
    alias(buildLibs.plugins.hilt)
    alias(buildLibs.plugins.room3)
    alias(buildLibs.plugins.ktlint)
    alias(buildLibs.plugins.kover)
}

val sharedTestDir = "src/sharedTest/java"

val releaseKey: Properties? =
    rootProject.file("keystore.properties").takeIf { it.isFile }?.let { file ->
        Properties().apply { file.inputStream().use { load(it) } }
    }

android {
    namespace = "com.jonesl7l.carinfg"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.jonesl7l.carinfg"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "DEFAULT_BASE_URL", "\"http://10.0.2.2:3000/\"")
    }

    signingConfigs {
        if (releaseKey != null) {
            create("release") {
                storeFile = file(releaseKey.getProperty("storeFile"))
                storePassword = releaseKey.getProperty("storePassword")
                keyAlias = releaseKey.getProperty("keyAlias")
                keyPassword = releaseKey.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            optimization {
                enable = false
            }
            signingConfig = signingConfigs.getByName(if (releaseKey != null) "release" else "debug")
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

    lint {
        disable += "OldTargetApi"
    }

    sourceSets {
        getByName("test").kotlin.directories.add(sharedTestDir)
        getByName("androidTest").kotlin.directories.add(sharedTestDir)
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

ktlint {
    version.set(buildLibs.versions.ktlint)
}

kover {
    currentProject {
        createVariant("app") {
            add("debug")
        }
    }
    reports {
        filters {
            excludes {
                classes(
                    "*.BuildConfig",
                    "*Hilt_*",
                    "*_HiltModules*",
                    "*_Factory",
                    "*_Factory\$*",
                    "*_MembersInjector",
                    "*_Impl",
                    "*_Impl\$*",
                    "*ComposableSingletons*",
                    "dagger.hilt.internal.*",
                    "hilt_aggregated_deps.*",
                )
                annotatedBy("androidx.compose.ui.tooling.preview.Preview")
            }
        }
    }
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
    implementation(libs.kotlinx.datetime)

    implementation(libs.bundles.coil)
    implementation(libs.play.services.location)
    implementation(libs.timber)

    ktlintRuleset(buildLibs.ktlint.compose.rules)

    testImplementation(testLibs.bundles.unit.test)

    debugImplementation(testLibs.androidx.compose.ui.test.manifest)
    androidTestImplementation(testLibs.bundles.android.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    kspAndroidTest(libs.hilt.compiler)
}

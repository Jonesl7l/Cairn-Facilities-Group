buildscript {
    dependencies {
        classpath(buildLibs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(buildLibs.plugins.android.application) apply false
    alias(buildLibs.plugins.kotlin.compose) apply false
    alias(buildLibs.plugins.kotlin.serialization) apply false
    alias(buildLibs.plugins.ksp) apply false
    alias(buildLibs.plugins.hilt) apply false
    alias(buildLibs.plugins.room3) apply false
    alias(buildLibs.plugins.spotless) apply false
    alias(buildLibs.plugins.kover) apply false
}

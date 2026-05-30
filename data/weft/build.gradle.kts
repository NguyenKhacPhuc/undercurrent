plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.weft.undercurrent.data.weft"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.navigation)
    implementation(projects.core.domain)
    implementation(projects.feature.creator)

    api("dev.weft:weft-runtime")
    api("dev.weft:weft-compose")
    api("dev.weft:weft-compose-defaults")
    api("dev.weft:weft-oauth")
    debugImplementation("dev.weft.devtools:weft-devtools")

    implementation(libs.kotlinx.coroutines.android)
}

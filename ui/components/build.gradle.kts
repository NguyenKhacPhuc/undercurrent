// :ui:components — Android-only WeftComponent palette.
//
// The ~46-component palette the agent picks from when emitting a
// `ui_render` tool call. Each component is a `WeftComponent<*>`
// subclass: serializable props in, ComponentEvent out, Compose
// rendering body in the middle. State is hoisted by the caller —
// these are pure views with no store / repo coupling.
//
// Lives in :ui:* (Android-only) rather than :core:ui (KMP) because
// the substrate's `dev.weft.compose.components.WeftComponent` base
// class is in :android-compose, which doesn't ship an iOS variant.
//
// Apply the stock `com.android.library` plugin (not KMP) because the
// dependency chain to :android-compose is JVM/Android-only.

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.weft.undercurrent.ui.components"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Theme tokens — UndercurrentTheme.colors / typography / shapes are
    // referenced by every visual component to honor the active palette.
    implementation(projects.core.designSystem)
    // TokenDivider + other shared composable primitives consumed by
    // AppDrawer (the host's side-navigation surface).
    implementation(projects.core.ui)
    // ConversationSummary — the AppDrawer renders the conversation list
    // from this shared gateway type.
    implementation(projects.shared)
    // Conversation grouping + relative-time formatting helpers consumed
    // by AppDrawer. Directional caveat: :ui:components depending on
    // :feature:conversations is unusual (features normally depend on
    // shared UI, not vice versa). The helpers in question are pure
    // data utilities though — if they grow, hoist them to :core:ext.
    implementation(projects.feature.conversations)

    // Weft substrate — WeftComponent base + Local* composition locals +
    // EmbedComponents (Html / WebView) re-exported from the
    // UndercurrentComponents factory.
    api("dev.weft:weft-android-compose")
    api("dev.weft:weft-android-compose-defaults")

    // Compose runtime + Material3 + extended icons (used by Tag, Stat,
    // KeyValue, etc).
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Coil — image rendering for PhotoFrame / Avatar / hero-style cards.
    // The undercurrentComponents() factory takes an ImageLoader so the
    // host can configure caching / network behavior at runtime.
    implementation(libs.coil.compose)

    implementation(libs.kotlinx.serialization.json)
}

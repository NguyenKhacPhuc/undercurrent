// KMP + Compose Multiplatform library. Use for shared UI primitives
// (loading, error, dialog), design tokens (colors, typography),
// resources (strings, drawables), and navigation primitives.

plugins {
    alias(libs.plugins.undercurrent.kmp.library)
    alias(libs.plugins.undercurrent.kmp.library.compose)
    // Needed for the @Serializable props data classes in
    // src/androidMain/kotlin/.../components/*Components.kt.
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Theme tokens + UndercurrentTheme accessor.
            api(projects.core.designSystem)
        }
        androidMain.dependencies {
            // androidMain hosts the WeftComponent palette (`ui_render`)
            // and the AppDrawer. They're Android-only because Weft's
            // substrate Compose layer (:android-compose) doesn't ship an
            // iOS variant — so the agent's rendered UI is Android-only
            // for v1. iosMain stays free of these and the iOS app
            // ships without the ui_render surface.
            api("dev.weft:weft-android-compose")
            api("dev.weft:weft-android-compose-defaults")
            // Coil for SubcomposeAsyncImage in PhotoFrame / Avatar /
            // hero-style components. The undercurrentComponents()
            // factory takes an ImageLoader so the host can configure
            // caching / network behavior at runtime.
            implementation(libs.coil.compose)
            // Extended Material icon set — components/Tokens.kt references
            // dozens of glyphs (Bookmark, OpenInNew, TrendingUp, …) that
            // aren't in the core icon set. Pulled in only on androidMain
            // because it's an Android-Compose-only artifact.
            implementation(project.dependencies.platform(libs.androidx.compose.bom))
            implementation(libs.androidx.compose.material.icons.extended)
            // ConversationSummary (from :shared) + the conversation
            // grouping helpers (hoisted out of :feature:conversations
            // into :core:ext) consumed by AppDrawer. Going through
            // :core:ext keeps the dependency direction one-way —
            // features depend on core, never the other way around.
            implementation(projects.shared)
            implementation(projects.core.ext)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.core.ui"
}

// :core:ui — KMP + Compose Multiplatform library.
//
// Houses two layers of shared UI:
//
//  1. Cross-platform UI primitives in commonMain (`ScreenScaffold`,
//     `SectionLabel`, `TokenDivider`, loading / error placeholders)
//     that pair with `:core:design-system`'s theme tokens.
//
//  2. The full WeftComponent palette + `AppDrawer` — also commonMain
//     since the substrate's `:android-compose` is KMP-published
//     (jvm + androidTarget + iosArm64 + iosSimulatorArm64). What WAS
//     androidMain-only is now mostly KMP-clean; only the embed
//     contribution (HtmlComponent / WebViewComponent, both backed by
//     `android.webkit.WebView`) stays Android-only — surfaced via an
//     `expect val platformEmbedComponents` so each target plugs in
//     whatever embed-style components it can ship.
//
// androidMain dependencies remaining: the substrate's
// `:android-compose-defaults` (for the EmbedComponents `actual`) and
// Coil's OkHttp network engine. iosMain is dep-free for now; the
// `platformEmbedComponents.ios.kt` returns an empty list.

plugins {
    alias(libs.plugins.undercurrent.kmp.library)
    alias(libs.plugins.undercurrent.kmp.library.compose)
    // For the @Serializable props data classes in
    // components/*Components.kt.
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Theme tokens + UndercurrentTheme accessor.
            api(projects.core.designSystem)
            // Substrate Compose layer (WeftComponent base + Local* +
            // ComposeUiBridge + TreeRenderer). KMP-published as of
            // weft@2ac7c49 — that's what made this commonMain move
            // possible. Stays `api` because callers reference
            // WeftComponent / ComponentEvent directly.
            api("dev.weft:weft-compose")
            // Coil 3 — KMP-friendly image loader for the
            // SubcomposeAsyncImage call sites in PhotoFrame / Avatar /
            // hero-style components. Network engine is per-platform
            // (OkHttp in androidMain; iOS would need ktor3 if it ever
            // serves remote images).
            implementation(libs.coil.compose)
            // Extended Material icon set — components/Tokens.kt
            // references dozens of glyphs (Bookmark, OpenInNew,
            // TrendingUp, …) that aren't in the core icon set. CMP
            // variant lives in `org.jetbrains.compose.material:*`.
            implementation(libs.compose.multiplatform.material.icons.extended)
            // ConversationSummary (from :shared) + the conversation
            // grouping helpers (in :core:ext) consumed by AppDrawer.
            implementation(projects.shared)
            implementation(projects.core.ext)
            implementation(libs.kotlinx.serialization.json)
            // Calendar / Countdown components use kotlinx-datetime +
            // kotlin.time for date math.
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            // EmbedComponents `actual` pulls HtmlComponent +
            // WebViewComponent from the substrate's defaults module.
            // Those wrap `android.webkit.WebView` so the dependency
            // stays Android-only — commonMain references them via
            // the `platformEmbedComponents` expect/actual.
            implementation("dev.weft:weft-compose-defaults")
            // Coil 3's Android HTTP engine. SubcomposeAsyncImage loads
            // remote URLs the agent emits; without this the loader
            // can't fetch.
            implementation(libs.coil.network.okhttp)
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.core.ui"
}

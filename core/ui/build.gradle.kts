// :core:ui — KMP + Compose Multiplatform library.
//
// Houses two layers of shared UI:
//
//  1. Cross-platform UI primitives in commonMain (`ScreenScaffold`,
//     `SectionLabel`, `TokenDivider`, loading / error placeholders)
//     that pair with `:core:design-system`'s theme tokens.
//
//  2. The full WeftComponent palette + `AppDrawer` — also commonMain
//     since the substrate's `:compose` is KMP-published (jvm +
//     androidTarget + iosArm64 + iosSimulatorArm64). The embed
//     contribution (HtmlComponent / WebViewComponent) now ships on
//     both targets via the substrate's `:compose-defaults` — Android
//     wraps `android.webkit.WebView`, iOS wraps `WKWebView`. Each
//     target plugs in via an `expect val platformEmbedComponents`.
//
// Per-target deps: `:compose-defaults` lands in both `androidMain`
// and `iosMain` (the EmbedComponents `actual` on each side). Coil's
// OkHttp network engine stays androidMain-only.

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
            implementation(projects.core.domain)
            implementation(projects.core.ext)
            implementation(libs.kotlinx.serialization.json)
            // Calendar / Countdown components use kotlinx-datetime +
            // kotlin.time for date math.
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            // EmbedComponents `actual` pulls HtmlComponent +
            // WebViewComponent from the substrate's defaults module
            // (Android impls wrap `android.webkit.WebView`).
            implementation("dev.weft:weft-compose-defaults")
            // Coil 3's Android HTTP engine. SubcomposeAsyncImage loads
            // remote URLs the agent emits; without this the loader
            // can't fetch.
            implementation(libs.coil.network.okhttp)
        }
        iosMain.dependencies {
            // Same substrate module on iOS — WKWebView-backed
            // HtmlComponent + WebViewComponent feed the
            // `platformEmbedComponents` actual.
            implementation("dev.weft:weft-compose-defaults")
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.core.ui"
}

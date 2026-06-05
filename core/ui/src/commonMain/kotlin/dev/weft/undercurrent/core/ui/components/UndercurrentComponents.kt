package dev.weft.undercurrent.core.ui.components

import coil3.ImageLoader
import dev.weft.compose.components.MiniAppActionInvoker
import dev.weft.compose.components.MiniAppAssistantHandler
import dev.weft.compose.components.MiniAppScopeResolver
import dev.weft.compose.components.MiniAppStateStore
import dev.weft.compose.components.WeftComponent

/**
 * Undercurrent's curated component palette for `ui_render`. Replaces the
 * substrate's built-in defaults (excluded via `includeDefaults = false`
 * on [dev.weft.compose.WeftUi]) so the agent sees only components that
 * match Undercurrent's editorial / document-style theme.
 *
 * Categories shipped today:
 *  - **Display (13)**: Text, Heading, Quote, Stat, Tag, Avatar, Note,
 *    Hairline, Banner, Empty, Skeleton, Steps, Markdown
 *  - **Layout (8)**: Stack, Inline, Section, Sheet, Grid, Reveal, ListRow,
 *    KeyValue, Tabs, Timeline
 *  - **Action (5)**: Button, Link, TapCard, IconAction, Choice
 *  - **Input (8)**: Field, Toggle, Stepper, Checklist, SegmentedToggle,
 *    Rating, MoodScale, DateStrip, Composer
 *  - **Charts (5)**: LineChart, BarChart, Sparkline, Donut, ProgressRing
 *  - **Content (5)**: Markdown, CodeBlock, Diff, Spotlight, PhotoFrame
 *
 * Total: ~46 components.
 *
 * All consume tokens from [dev.weft.undercurrent.core.designsystem.UndercurrentTheme]
 * so palette / typography / shape swaps in Settings apply automatically.
 */
fun undercurrentComponents(
    imageLoader: ImageLoader,
    miniAppInvoker: MiniAppActionInvoker? = null,
    miniAppScopeResolver: MiniAppScopeResolver? = null,
    miniAppStateStore: MiniAppStateStore? = null,
    miniAppAssistant: MiniAppAssistantHandler? = null,
): List<WeftComponent<*>> =
    displayComponents(imageLoader) +
        undercurrentLayoutComponents +
        undercurrentActionComponents +
        undercurrentInputComponents +
        undercurrentFeedbackComponents +
        undercurrentChartComponents +
        undercurrentListComponents +
        contentComponents(imageLoader) +
        undercurrentAdvancedInputComponents +
        undercurrentTabsComponents +
        // ── Second wave ──────────────────────────────────────────────
        undercurrentFormsPlusComponents +
        undercurrentCalendarComponents +
        undercurrentDataVizPlusComponents +
        undercurrentProductivityComponents +
        undercurrentNavComponents +
        metaComponents(imageLoader) +
        // ── Third wave ───────────────────────────────────────────────
        mediaComponents(imageLoader) +
        undercurrentFormPlus2Components +
        commerceComponents(imageLoader) +
        undercurrentOverlayComponents +
        undercurrentSignalsComponents +
        // ── Fourth wave ──────────────────────────────────────────────
        heroComponents(imageLoader) +
        undercurrentEventComponents +
        undercurrentPricingComponents +
        undercurrentStructuredListComponents +
        // ── Fifth wave (primitives) ─────────────────────────────────
        undercurrentPrimitivesComponents +
        // ── Sixth wave — travel ─────────────────────────────────────
        travelComponents(imageLoader) +
        // ── Seventh wave — health ───────────────────────────────────
        undercurrentHealthComponents +
        // ── Eighth wave — data ──────────────────────────────────────
        undercurrentDataComponents +
        // ── Ninth wave — social ─────────────────────────────────────
        socialComponents(imageLoader) +
        // ── Tenth wave — music + games ──────────────────────────────
        musicGamesComponents(imageLoader) +
        // ── Eleventh wave — embed (HTML / WebView) ──────────────────
        // Platform-specific (android.webkit.WebView / WKWebView). The
        // bridge bindings (invoker, scope resolver, state store) make a
        // script-enabled HTML mini-app's window.weft bridge live; passed
        // null they stay sandboxed. Without these in the registry the
        // agent can only dump HTML into a Text block.
        platformEmbedComponents(miniAppInvoker, miniAppScopeResolver, miniAppStateStore, miniAppAssistant)

package dev.weft.undercurrent.ui.components

import coil.ImageLoader
import dev.weft.compose.components.EmbedComponents
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
fun undercurrentComponents(imageLoader: ImageLoader): List<WeftComponent<*>> =
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
        // Lifted from the substrate's :android-compose-defaults rather than
        // re-implemented here. HtmlComponent (with runScripts=true) is the
        // path for self-contained interactive widgets — calculators,
        // countdowns, mini-games (snake, tic-tac-toe). Without these in the
        // registry the agent has no choice but to dump HTML source into a
        // Text block.
        EmbedComponents

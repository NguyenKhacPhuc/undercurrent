package dev.weft.undercurrent.core.model

/**
 * The four shipping palettes — picked by the user via Settings, drives
 * the Compose color scheme. Pure enum (name + display strings); the
 * Compose color tables that back each palette live in
 * `:core:design-system` as an extension on this enum.
 *
 * Color choices come from the round-1 / round-2 palette explorations.
 * When tweaking, regenerate the preview HTML to compare side-by-side.
 *
 * Default-on-first-launch is [Vellum] (no chromatic accent — calmest).
 * [Newsprint] is the highest-contrast for outdoor/bright use;
 * [WarmDarkAmber] is the most "writerly"; [SageOchre] is the most
 * distinct.
 *
 * KMP — commonMain. Moved from `app/.../theme/Palettes.kt`. The
 * `colors(dark)` accessor that returned `UndercurrentColors` did NOT
 * come along — it depends on Compose's `Color` type, so it lives in
 * `:core:design-system` as `AppPalette.colors(dark)` extension.
 */
enum class AppPalette(
    val displayName: String,
    val tagline: String,
) {
    WarmDarkAmber(
        displayName = "Warm dark",
        tagline = "Amber accent. Calm, writerly.",
    ),
    SageOchre(
        displayName = "Sage & ochre",
        tagline = "Quieter, more distinct.",
    ),
    Newsprint(
        displayName = "Newsprint",
        tagline = "High contrast. Editorial red.",
    ),
    Vellum(
        displayName = "Vellum",
        tagline = "No accent. Pure writing surface.",
    );

    companion object {
        val Default: AppPalette = Vellum
    }
}

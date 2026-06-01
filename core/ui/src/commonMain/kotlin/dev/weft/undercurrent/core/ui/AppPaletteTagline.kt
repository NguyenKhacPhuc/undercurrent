package dev.weft.undercurrent.core.ui

import dev.weft.undercurrent.core.model.AppPalette
import dev.weft.undercurrent.core.resources.Res
import dev.weft.undercurrent.core.resources.palette_tagline_newsprint
import dev.weft.undercurrent.core.resources.palette_tagline_sage_ochre
import dev.weft.undercurrent.core.resources.palette_tagline_vellum
import dev.weft.undercurrent.core.resources.palette_tagline_warm_dark
import org.jetbrains.compose.resources.StringResource

/**
 * The localized one-line tagline shown under each palette in the theme
 * pickers (Settings → Appearance, and the in-chat palette sheet).
 *
 * Lives here rather than on the [AppPalette] enum because `:core:model`
 * is resource-free commonMain — the enum carries only structural data,
 * and the UI resolves display copy via `stringResource(palette.taglineRes())`.
 * Mirrors the `ProviderKind.taglineRes()` pattern in onboarding.
 */
fun AppPalette.taglineRes(): StringResource = when (this) {
    AppPalette.WarmDarkAmber -> Res.string.palette_tagline_warm_dark
    AppPalette.SageOchre -> Res.string.palette_tagline_sage_ochre
    AppPalette.Newsprint -> Res.string.palette_tagline_newsprint
    AppPalette.Vellum -> Res.string.palette_tagline_vellum
}

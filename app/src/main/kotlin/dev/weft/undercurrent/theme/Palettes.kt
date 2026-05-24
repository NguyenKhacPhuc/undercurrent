package dev.weft.undercurrent.theme

import androidx.compose.ui.graphics.Color

/**
 * The four shipping palettes. Each defines a [light] and [dark] variant of
 * [UndercurrentColors]; [UndercurrentTheme] picks the right one based on
 * the user's mode preference + system dark setting.
 *
 * Color choices come from the round-1 / round-2 palette explorations
 * (`/tmp/undercurrent-palettes*.html`). When tweaking, regenerate the
 * preview HTML first to compare side-by-side.
 *
 * Default-on-first-launch is [WarmDarkAmber] (C). [Newsprint] (F) is the
 * highest-contrast for outdoor/bright use; [Vellum] (H) is the calmest /
 * no chromatic accent; [SageOchre] (D) is the most distinct.
 */
internal enum class AppPalette(
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

    fun colors(dark: Boolean): UndercurrentColors = when (this) {
        WarmDarkAmber -> if (dark) WarmDarkAmberDark else WarmDarkAmberLight
        SageOchre -> if (dark) SageOchreDark else SageOchreLight
        Newsprint -> if (dark) NewsprintDark else NewsprintLight
        Vellum -> if (dark) VellumDark else VellumLight
    }

    companion object {
        val Default: AppPalette = Vellum
    }
}

// ─────────────────────────────────────────────────────────────────────────
// C — Warm dark-first (amber)
// ─────────────────────────────────────────────────────────────────────────

private val WarmDarkAmberDark = UndercurrentColors(
    background = Color(0xFF1C1A17),
    surface = Color(0xFF221F1B),
    surfaceMuted = Color(0xFF25221E),
    ink = Color(0xFFECE6DE),
    inkMuted = Color(0xFFA8A299),
    inkSubtle = Color(0xFF6E6962),
    divider = Color(0xFF2E2A25),
    accent = Color(0xFFE8A547),
    onAccent = Color(0xFF1C1A17),
    codeBg = Color(0xFF25221E),
    codeInk = Color(0xFFC9C3BB),
    codeBorder = Color(0xFF2E2A25),
    error = Color(0xFFE57373),
    isDark = true,
)

private val WarmDarkAmberLight = UndercurrentColors(
    background = Color(0xFFFAF7F2),
    surface = Color(0xFFFFFEFB),
    surfaceMuted = Color(0xFFF0EBE1),
    ink = Color(0xFF2C2825),
    inkMuted = Color(0xFF6B6660),
    inkSubtle = Color(0xFF9A958D),
    divider = Color(0xFFE5DFD2),
    accent = Color(0xFFB47A1F),
    onAccent = Color(0xFFFFFFFF),
    codeBg = Color(0xFFF0EBE1),
    codeInk = Color(0xFF4A4541),
    codeBorder = Color(0xFFE5DFD2),
    error = Color(0xFFB3261E),
    isDark = false,
)

// ─────────────────────────────────────────────────────────────────────────
// D — Sage + ochre
// ─────────────────────────────────────────────────────────────────────────

private val SageOchreLight = UndercurrentColors(
    background = Color(0xFFF4F2EB),
    surface = Color(0xFFFAF8F2),
    surfaceMuted = Color(0xFFEAE7DD),
    ink = Color(0xFF1F2421),
    inkMuted = Color(0xFF5F6862),
    inkSubtle = Color(0xFF8F968F),
    divider = Color(0xFFDCD8CB),
    accent = Color(0xFF6B8E7F),
    onAccent = Color(0xFFFFFFFF),
    codeBg = Color(0xFFEAE7DD),
    codeInk = Color(0xFF2D3330),
    codeBorder = Color(0xFFDCD8CB),
    error = Color(0xFFB3261E),
    isDark = false,
)

private val SageOchreDark = UndercurrentColors(
    background = Color(0xFF161A18),
    surface = Color(0xFF1B201D),
    surfaceMuted = Color(0xFF1D2220),
    ink = Color(0xFFDDE2DE),
    inkMuted = Color(0xFF9CA39E),
    inkSubtle = Color(0xFF676E69),
    divider = Color(0xFF232927),
    accent = Color(0xFF8FB39F),
    onAccent = Color(0xFF161A18),
    codeBg = Color(0xFF1D2220),
    codeInk = Color(0xFFBFC4C0),
    codeBorder = Color(0xFF232927),
    error = Color(0xFFE57373),
    isDark = true,
)

// ─────────────────────────────────────────────────────────────────────────
// F — Newsprint
// ─────────────────────────────────────────────────────────────────────────

private val NewsprintLight = UndercurrentColors(
    background = Color(0xFFF8F5EF),
    surface = Color(0xFFFFFCF6),
    surfaceMuted = Color(0xFFEDE9E0),
    ink = Color(0xFF0A0A0A),
    inkMuted = Color(0xFF555350),
    inkSubtle = Color(0xFF8A8884),
    divider = Color(0xFFDDD8CC),
    accent = Color(0xFFB91C1C),
    onAccent = Color(0xFFFFFFFF),
    codeBg = Color(0xFFEDE9E0),
    codeInk = Color(0xFF1F1F1F),
    codeBorder = Color(0xFFDDD8CC),
    error = Color(0xFFB91C1C),
    isDark = false,
)

private val NewsprintDark = UndercurrentColors(
    background = Color(0xFF0A0A0A),
    surface = Color(0xFF101010),
    surfaceMuted = Color(0xFF141414),
    ink = Color(0xFFF0EBE0),
    inkMuted = Color(0xFFAEA9A0),
    inkSubtle = Color(0xFF6E6A63),
    divider = Color(0xFF1F1F1F),
    accent = Color(0xFFEF4444),
    onAccent = Color(0xFF0A0A0A),
    codeBg = Color(0xFF141414),
    codeInk = Color(0xFFD4CFC5),
    codeBorder = Color(0xFF1F1F1F),
    error = Color(0xFFEF4444),
    isDark = true,
)

// ─────────────────────────────────────────────────────────────────────────
// H — Vellum / sepia (no chromatic accent — accent is a darker ink tone)
// ─────────────────────────────────────────────────────────────────────────

private val VellumLight = UndercurrentColors(
    background = Color(0xFFF5EFE0),
    surface = Color(0xFFFAF5E8),
    surfaceMuted = Color(0xFFECE4D2),
    ink = Color(0xFF3A2E1F),
    inkMuted = Color(0xFF756448),
    inkSubtle = Color(0xFFA89878),
    divider = Color(0xFFDCD2BB),
    // Accent is a deeper sepia — no chromatic shift, just stronger ink.
    accent = Color(0xFF3A2E1F),
    onAccent = Color(0xFFF5EFE0),
    codeBg = Color(0xFFECE4D2),
    codeInk = Color(0xFF4A3D2A),
    codeBorder = Color(0xFFDCD2BB),
    error = Color(0xFF8B2D2D),
    isDark = false,
)

private val VellumDark = UndercurrentColors(
    background = Color(0xFF1A1612),
    surface = Color(0xFF1F1B16),
    surfaceMuted = Color(0xFF221D17),
    ink = Color(0xFFD9CFB8),
    inkMuted = Color(0xFF9A917D),
    inkSubtle = Color(0xFF6A6353),
    divider = Color(0xFF2A241C),
    accent = Color(0xFFD9CFB8),
    onAccent = Color(0xFF1A1612),
    codeBg = Color(0xFF221D17),
    codeInk = Color(0xFFB8AE96),
    codeBorder = Color(0xFF2A241C),
    error = Color(0xFFD9908D),
    isDark = true,
)

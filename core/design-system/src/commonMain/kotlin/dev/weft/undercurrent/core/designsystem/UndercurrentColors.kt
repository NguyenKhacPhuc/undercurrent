package dev.weft.undercurrent.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Semantic color tokens for Undercurrent's themes. Kept lean (~12
 * slots) — the long tail of Material3's ColorScheme is derived from
 * these by `toMaterialColorScheme`.
 *
 * Naming convention:
 *  - `*Bg` — surface fills (background of a region)
 *  - `ink*` — text colors, ordered from most-emphasized to least
 *  - `accent*` — the single chromatic accent (or near-neutral, for Vellum)
 *  - `code*` — code-block specific
 *
 * KMP — commonMain. Moved from `app/.../theme/UndercurrentColors.kt`.
 * `androidx.compose.ui.graphics.Color` is CMP-friendly; the data
 * class compiles on both Android + iOS targets.
 */
@Immutable
data class UndercurrentColors(
    /** Page background — the canvas under everything. */
    val background: Color,
    /** Slightly raised surface (drawer, sheets, cards). */
    val surface: Color,
    /** Softer surface for grouped content (code blocks, callouts). */
    val surfaceMuted: Color,
    /** Primary text. */
    val ink: Color,
    /** Secondary text (role labels, timestamps, tool events). */
    val inkMuted: Color,
    /** Tertiary text (disabled states, very subtle hints). */
    val inkSubtle: Color,
    /** Hair-thin separators, outlines. */
    val divider: Color,
    /** The single accent color (links, primary buttons, active state). */
    val accent: Color,
    /** Text/icon on top of [accent]. */
    val onAccent: Color,
    /** Code block fill. */
    val codeBg: Color,
    /** Code block text. */
    val codeInk: Color,
    /** Code block border (often slightly darker than [codeBg]). */
    val codeBorder: Color,
    /** Error color (validation messages, failed sends). */
    val error: Color,
    /** True if this palette is intended to be read as a dark scheme. */
    val isDark: Boolean,
)

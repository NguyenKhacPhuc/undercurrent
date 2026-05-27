package dev.weft.undercurrent.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography tokens. Three families:
 *  - [serifBody] / [serifBodyLarge] — message content (document feel).
 *    Uses `FontFamily.Serif` which Android resolves to the system
 *    serif (Noto Serif on most devices). On iOS via CMP, the system
 *    default serif (New York / Georgia) is used.
 *  - [sansHeader] / [sansLabel] / [sansSmall] — UI chrome.
 *  - [mono] — code blocks. System monospace.
 *
 * Swap to bundled fonts later by changing the family in [Default].
 *
 * KMP — commonMain. Moved from `app/.../theme/UndercurrentTypography.kt`.
 */
@Immutable
public data class UndercurrentTypography(
    val serifBody: TextStyle,
    val serifBodyLarge: TextStyle,
    val sansHeader: TextStyle,
    val sansLabel: TextStyle,
    val sansSmall: TextStyle,
    val mono: TextStyle,
) {
    public companion object {
        public val Default: UndercurrentTypography = UndercurrentTypography(
            serifBody = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.sp,
            ),
            serifBodyLarge = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp,
            ),
            sansHeader = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                letterSpacing = (-0.1).sp,
            ),
            // Role labels ("You", "Undercurrent") — small caps feel.
            sansLabel = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.5.sp,
            ),
            sansSmall = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.sp,
            ),
            mono = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                letterSpacing = 0.sp,
            ),
        )
    }
}

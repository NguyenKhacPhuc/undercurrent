package dev.weft.undercurrent.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

/**
 * Corner shapes used across the app.
 *
 * Most palettes use [DefaultRounded]. Mono brutalist (not currently shipped
 * but reserved for future) would use [Square] for everything — switching
 * shape style alone changes the entire app's character.
 */
@Immutable
internal data class UndercurrentShapes(
    val xsmall: RoundedCornerShape,
    val small: RoundedCornerShape,
    val medium: RoundedCornerShape,
    val large: RoundedCornerShape,
    val xlarge: RoundedCornerShape,
) {
    companion object {
        val DefaultRounded: UndercurrentShapes = UndercurrentShapes(
            xsmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(16.dp),
            xlarge = RoundedCornerShape(24.dp),
        )
    }
}

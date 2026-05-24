package dev.weft.undercurrent.theme

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Root theme wrapper. Apply once near the top of the composition.
 *
 * Maps Undercurrent's small token set onto Material3's larger ColorScheme
 * so existing M3 components (Button, Card, IconButton, SnackbarHost, …)
 * inherit colors automatically — no per-component restyling needed.
 *
 * Tokens that M3 doesn't have a slot for (the serif body style, the code
 * block background, etc.) are exposed via [LocalUndercurrentColors] /
 * [LocalUndercurrentTypography] / [LocalUndercurrentShapes]. Access them
 * via the [UndercurrentTheme] accessor object inside any composable.
 */
@Composable
internal fun UndercurrentTheme(
    palette: AppPalette = AppPalette.Default,
    darkMode: Boolean = isSystemInDarkTheme(),
    typography: UndercurrentTypography = UndercurrentTypography.Default,
    shapes: UndercurrentShapes = UndercurrentShapes.DefaultRounded,
    content: @Composable () -> Unit,
) {
    val m3Typography = typography.toMaterialTypography()
    val m3Shapes = shapes.toMaterialShapes()

    // Crossfade on (palette + darkMode) so theme switches fade rather than
    // pop. State *inside* the content keeps its identity across the swap
    // because Crossfade reuses the same lambda body; only the rendered
    // colors blend. Switching is cheap because we don't recreate the
    // composition tree, only re-resolve the color tokens.
    Crossfade(
        targetState = palette to darkMode,
        animationSpec = tween(durationMillis = 280),
        label = "theme-crossfade",
    ) { (currentPalette, currentDark) ->
        val colors = currentPalette.colors(currentDark)
        val m3Scheme = colors.toMaterialColorScheme()
        CompositionLocalProvider(
            LocalUndercurrentColors provides colors,
            LocalUndercurrentTypography provides typography,
            LocalUndercurrentShapes provides shapes,
        ) {
            MaterialTheme(
                colorScheme = m3Scheme,
                typography = m3Typography,
                shapes = m3Shapes,
                content = content,
            )
        }
    }
}

/**
 * Accessor for Undercurrent's design tokens inside any composable scope
 * underneath [UndercurrentTheme]. Mirrors the pattern of [MaterialTheme].
 *
 * Usage:
 * ```
 * Text(
 *     text = "Hello",
 *     style = UndercurrentTheme.typography.serifBody,
 *     color = UndercurrentTheme.colors.ink,
 * )
 * ```
 */
internal object UndercurrentTheme {
    val colors: UndercurrentColors
        @Composable
        @ReadOnlyComposable
        get() = LocalUndercurrentColors.current

    val typography: UndercurrentTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalUndercurrentTypography.current

    val shapes: UndercurrentShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalUndercurrentShapes.current
}

internal val LocalUndercurrentColors = staticCompositionLocalOf<UndercurrentColors> {
    error("UndercurrentColors not provided — wrap your composition in UndercurrentTheme")
}

internal val LocalUndercurrentTypography = staticCompositionLocalOf<UndercurrentTypography> {
    error("UndercurrentTypography not provided — wrap your composition in UndercurrentTheme")
}

internal val LocalUndercurrentShapes = staticCompositionLocalOf<UndercurrentShapes> {
    error("UndercurrentShapes not provided — wrap your composition in UndercurrentTheme")
}

// ─────────────────────────────────────────────────────────────────────────
// Token → Material3 mapping
// ─────────────────────────────────────────────────────────────────────────

/**
 * Project our small token set onto Material3's much-larger ColorScheme.
 *
 * The mapping decisions:
 *  - M3 `primary` ← our `accent` (button fills, FAB, active state)
 *  - M3 `surface`/`background` ← ours of same name
 *  - M3 `surfaceVariant` ← our `surfaceMuted` (cards, code blocks)
 *  - M3 `outline` ← our `divider`
 *  - M3 `secondary`/`tertiary` slots reuse `accent` so any component
 *    that asks for them gets a coherent color (Material chooses which
 *    based on the component — we don't differentiate per role).
 */
private fun UndercurrentColors.toMaterialColorScheme() = if (isDark) {
    darkColorScheme(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accent,
        onPrimaryContainer = onAccent,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = surfaceMuted,
        onSecondaryContainer = ink,
        tertiary = accent,
        onTertiary = onAccent,
        tertiaryContainer = surfaceMuted,
        onTertiaryContainer = ink,
        background = background,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surfaceMuted,
        onSurfaceVariant = inkMuted,
        surfaceTint = accent,
        outline = divider,
        outlineVariant = divider,
        error = error,
        onError = onAccent,
        errorContainer = error,
        onErrorContainer = onAccent,
        inverseSurface = ink,
        inverseOnSurface = background,
        inversePrimary = accent,
        scrim = ink,
    )
} else {
    lightColorScheme(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accent,
        onPrimaryContainer = onAccent,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = surfaceMuted,
        onSecondaryContainer = ink,
        tertiary = accent,
        onTertiary = onAccent,
        tertiaryContainer = surfaceMuted,
        onTertiaryContainer = ink,
        background = background,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surfaceMuted,
        onSurfaceVariant = inkMuted,
        surfaceTint = accent,
        outline = divider,
        outlineVariant = divider,
        error = error,
        onError = onAccent,
        errorContainer = error,
        onErrorContainer = onAccent,
        inverseSurface = ink,
        inverseOnSurface = background,
        inversePrimary = accent,
        scrim = ink,
    )
}

/**
 * Map our typography onto M3's [Typography] so default-styled M3 widgets
 * (Button, Text without explicit style, AlertDialog title, etc.) pick up
 * our families and weights.
 */
private fun UndercurrentTypography.toMaterialTypography() = Typography(
    displayLarge = serifBodyLarge,
    displayMedium = serifBodyLarge,
    displaySmall = serifBodyLarge,
    headlineLarge = sansHeader,
    headlineMedium = sansHeader,
    headlineSmall = sansHeader,
    titleLarge = sansHeader,
    titleMedium = sansHeader,
    titleSmall = sansHeader,
    bodyLarge = serifBody,
    bodyMedium = serifBody,
    bodySmall = sansSmall,
    labelLarge = sansLabel,
    labelMedium = sansLabel,
    labelSmall = sansLabel,
)

private fun UndercurrentShapes.toMaterialShapes() = Shapes(
    extraSmall = xsmall,
    small = small,
    medium = medium,
    large = large,
    extraLarge = xlarge,
)

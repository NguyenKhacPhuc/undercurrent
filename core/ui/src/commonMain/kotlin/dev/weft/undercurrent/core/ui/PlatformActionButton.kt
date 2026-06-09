package dev.weft.undercurrent.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Sample of the `expect`/`actual` **Composable** seam: one shared
 * contract (label + click, on-brand tokens), two platform renderings.
 *
 * Reach for this when the same logical control should feel native on each
 * platform rather than identical everywhere:
 *   - Android — `PlatformActionButton.android.kt`: filled Material 3
 *     button (ripple, elevation-style container, rounded corners).
 *   - iOS — `PlatformActionButton.ios.kt`: flat accent **capsule** (pill),
 *     no Material ripple, mirroring UIKit button styling.
 *
 * Callers in `commonMain` just invoke `PlatformActionButton(...)` — they
 * never branch on platform, and the stateless/previewable contract from
 * the UI rules still holds: state in, callback out. The `@Preview`s live
 * in each `actual` file, since an `expect` declaration has no body to
 * render — Android renders the Material look, iOS the pill.
 *
 * Use this seam only when the *rendering* genuinely differs. For a small
 * tweak (one padding value, one extra row), prefer an `expect/actual val`
 * platform flag and an inline branch instead of splitting the whole
 * Composable.
 */
@Composable
expect fun PlatformActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
)

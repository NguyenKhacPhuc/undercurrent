package dev.weft.undercurrent.core.model

/**
 * User's theme preferences. Persisted via the host's theme repository
 * (Android: DataStore; iOS: NSUserDefaults — host wiring decides).
 * Mirrored into AppState so the UI can react.
 *
 * KMP — commonMain. Moved from `app/.../theme/ThemeMode.kt`.
 */
data class ThemePrefs(
    val palette: AppPalette = AppPalette.Default,
    val mode: ThemeMode = ThemeMode.Default,
) {
    companion object {
        val Default: ThemePrefs = ThemePrefs()
    }
}

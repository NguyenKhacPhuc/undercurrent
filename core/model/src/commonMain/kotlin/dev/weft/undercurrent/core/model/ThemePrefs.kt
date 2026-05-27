package dev.weft.undercurrent.core.model

/**
 * User's theme preferences. Persisted via the host's theme repository
 * (Android: DataStore; iOS: NSUserDefaults — host wiring decides).
 * Mirrored into AppState so the UI can react.
 *
 * KMP — commonMain. Moved from `app/.../theme/ThemeMode.kt`.
 */
public data class ThemePrefs(
    public val palette: AppPalette = AppPalette.Default,
    public val mode: ThemeMode = ThemeMode.Default,
) {
    public companion object {
        public val Default: ThemePrefs = ThemePrefs()
    }
}

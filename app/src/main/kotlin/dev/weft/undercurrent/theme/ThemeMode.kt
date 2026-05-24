package dev.weft.undercurrent.theme

/**
 * How [AppPalette] resolves to light vs dark colors.
 *
 *  - [Auto] — follow the system's dark mode setting.
 *  - [Light] — always render the light variant, regardless of system.
 *  - [Dark] — always render the dark variant.
 *
 * Independent of [AppPalette]: the user picks a palette (visual identity)
 * and separately picks how to handle light/dark (system behavior).
 */
internal enum class ThemeMode(val displayName: String) {
    Auto("Auto"),
    Light("Light"),
    Dark("Dark");

    companion object {
        val Default: ThemeMode = Auto
    }
}

/**
 * User's theme preferences. Persisted via [ThemeRepository] and mirrored
 * into [AppState.themePrefs] so the UI can react.
 */
internal data class ThemePrefs(
    val palette: AppPalette = AppPalette.Default,
    val mode: ThemeMode = ThemeMode.Default,
) {
    companion object {
        val Default: ThemePrefs = ThemePrefs()
    }
}

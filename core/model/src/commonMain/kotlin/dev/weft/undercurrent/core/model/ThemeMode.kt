package dev.weft.undercurrent.core.model

/**
 * How [AppPalette] resolves to light vs dark colors.
 *
 *  - [Auto] — follow the system's dark mode setting.
 *  - [Light] — always render the light variant, regardless of system.
 *  - [Dark] — always render the dark variant.
 *
 * Independent of [AppPalette]: the user picks a palette (visual
 * identity) and separately picks how to handle light/dark (system
 * behavior).
 *
 * KMP — commonMain. Moved from `app/.../theme/ThemeMode.kt`.
 */
public enum class ThemeMode(public val displayName: String) {
    Auto("Auto"),
    Light("Light"),
    Dark("Dark");

    public companion object {
        public val Default: ThemeMode = Auto
    }
}

package dev.weft.undercurrent.tools

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.serialization.KotlinTypeToken
import dev.weft.tools.WeftContext
import dev.weft.tools.WeftTool
import dev.weft.undercurrent.core.model.AppPalette
import dev.weft.undercurrent.core.model.ThemeMode
import dev.weft.undercurrent.core.domain.ThemeRepository
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

/**
 * Agent-facing tools that change the app's visual theme during a chat
 * turn. Persistence flows through [ThemeRepository]; the UI re-collects
 * via DataStore and updates on the next composition.
 *
 * Tokens are user-friendly slugs (`warm_dark`) rather than internal
 * enum names — the LLM tends to echo the slug back to the user.
 */

internal class SetThemePaletteTool(
    ctx: WeftContext,
    private val themeRepo: ThemeRepository,
) : WeftTool<SetThemePaletteTool.Args, String>(
    ctx = ctx,
    argsType = KotlinTypeToken(typeOf<Args>()),
    resultType = KotlinTypeToken(typeOf<String>()),
    descriptor = ToolDescriptor(
        name = "set_theme_palette",
        description = "Change the app's color palette. Call when the user asks to switch " +
            "palette, change colors, try a different look, or names one of the four " +
            "palettes by slug or display name. Available palettes:\n" +
            "  - warm_dark   — Warm dark. Amber accent. Calm, writerly.\n" +
            "  - sage_ochre  — Sage & ochre. Quieter, more distinct.\n" +
            "  - newsprint   — Newsprint. High contrast. Editorial red.\n" +
            "  - vellum      — Vellum. No accent. Pure writing surface.",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                "palette",
                "One of: warm_dark, sage_ochre, newsprint, vellum.",
                ToolParameterType.Enum(
                    arrayOf("warm_dark", "sage_ochre", "newsprint", "vellum"),
                ),
            ),
        ),
        optionalParameters = emptyList(),
    ),
    sideEffecting = true,
) {
    @Serializable
    internal data class Args(val palette: String)

    override suspend fun executeWeft(args: Args): String {
        val palette = paletteFromSlug(args.palette)
            ?: return "Unknown palette '${args.palette}'. Try warm_dark, sage_ochre, newsprint, or vellum."
        themeRepo.setPalette(palette)
        return "Palette set to ${palette.displayName}."
    }

    private fun paletteFromSlug(slug: String): AppPalette? =
        when (slug.lowercase().replace(" ", "_").replace("&", "")) {
            "warm_dark", "warm_dark_amber", "warmdark", "warm" -> AppPalette.WarmDarkAmber
            "sage_ochre", "sage__ochre", "sage_and_ochre", "sage", "ochre" -> AppPalette.SageOchre
            "newsprint" -> AppPalette.Newsprint
            "vellum" -> AppPalette.Vellum
            else -> null
        }
}

internal class SetThemeModeTool(
    ctx: WeftContext,
    private val themeRepo: ThemeRepository,
) : WeftTool<SetThemeModeTool.Args, String>(
    ctx = ctx,
    argsType = KotlinTypeToken(typeOf<Args>()),
    resultType = KotlinTypeToken(typeOf<String>()),
    descriptor = ToolDescriptor(
        name = "set_theme_mode",
        description = "Switch the app between light, dark, and system-following theme " +
            "mode. Call when the user asks to go dark / switch to light / follow the " +
            "system theme. Independent of the palette — works with any palette.",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                "mode",
                "One of: auto (follow system), light, or dark.",
                ToolParameterType.Enum(arrayOf("auto", "light", "dark")),
            ),
        ),
        optionalParameters = emptyList(),
    ),
    sideEffecting = true,
) {
    @Serializable
    internal data class Args(val mode: String)

    override suspend fun executeWeft(args: Args): String {
        val mode = when (args.mode.lowercase()) {
            "auto", "system" -> ThemeMode.Auto
            "light" -> ThemeMode.Light
            "dark" -> ThemeMode.Dark
            else -> return "Unknown mode '${args.mode}'. Try auto, light, or dark."
        }
        themeRepo.setMode(mode)
        return "Theme mode set to ${mode.displayName}."
    }
}

package dev.weft.undercurrent.data.weft.tools

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.serialization.KotlinTypeToken
import dev.weft.tools.WeftContext
import dev.weft.tools.WeftTool
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

/**
 * Open the user's preferred maps app with a single pin at the given
 * coordinates. The substrate's built-in `maps_open_directions` tool
 * always requires a *destination* — it's wrong for "just show this
 * point" use cases. This tool fills that gap.
 *
 * Implementation: build a standard Android `geo:` URI and hand it to
 * `os.intents.openUrl(...)`, which fires `Intent.ACTION_VIEW`. Any
 * installed maps app that registers the `geo` scheme handles it.
 *
 *   geo:LAT,LNG?q=LAT,LNG(LABEL)
 *
 * KMP — Android-only. Moved from
 * `app/.../features/maps/ShowLocationOnMapTool.kt`. The "maps
 * feature" is just this one Weft tool — there's no UI to migrate to
 * commonMain — so it lives directly in `:data:weft` (where every
 * Weft tool belongs per the migration playbook).
 */
public class ShowLocationOnMapTool(ctx: WeftContext) : WeftTool<ShowLocationOnMapTool.Args, String>(
    ctx = ctx,
    argsType = KotlinTypeToken(typeOf<Args>()),
    resultType = KotlinTypeToken(typeOf<String>()),
    descriptor = ToolDescriptor(
        name = "open_map",
        description = "Open the map app pinned at the given coordinates. " +
            "Call this whenever the user wants to see a location on a map. " +
            "Pair with location_current (for 'my location') or with " +
            "geocoded coords (for any address). Do NOT use for directions — " +
            "use maps_open_directions for A→B navigation.",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                "latitude",
                "Latitude in decimal degrees, e.g. 37.7749.",
                ToolParameterType.Float,
            ),
            ToolParameterDescriptor(
                "longitude",
                "Longitude in decimal degrees, e.g. -122.4194.",
                ToolParameterType.Float,
            ),
        ),
        optionalParameters = listOf(
            ToolParameterDescriptor(
                "label",
                "Short text to show on the pin, e.g. 'Your location' or " +
                    "'1 Infinite Loop'. Defaults to 'Location' if omitted.",
                ToolParameterType.String,
            ),
        ),
    ),
    sideEffecting = true,
) {

    @Serializable
    public data class Args(
        val latitude: Double,
        val longitude: Double,
        val label: String? = null,
    )

    override suspend fun executeWeft(args: Args): String {
        android.util.Log.d(
            "UndercurrentMaps",
            "open_map called: lat=${args.latitude}, lng=${args.longitude}, label=${args.label}",
        )

        if (args.latitude !in -90.0..90.0) {
            return "Invalid latitude ${args.latitude} (must be -90 to 90)."
        }
        if (args.longitude !in -180.0..180.0) {
            return "Invalid longitude ${args.longitude} (must be -180 to 180)."
        }

        val displayLabel = (args.label?.takeIf { it.isNotBlank() } ?: "Location")
            .replace(Regex("[(),]"), " ")
            .trim()

        val uri = "geo:${args.latitude},${args.longitude}" +
            "?q=${args.latitude},${args.longitude}($displayLabel)"
        val opened = os.intents.openUrl(uri, inApp = false)
        return if (opened) {
            "Opened the map at ${args.latitude}, ${args.longitude} ($displayLabel)."
        } else {
            "Could not open the map — no maps app appears to be installed."
        }
    }
}

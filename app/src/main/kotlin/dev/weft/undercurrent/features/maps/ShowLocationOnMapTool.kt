package dev.weft.undercurrent.features.maps

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
 * point" use cases ("show me my location on a map", "where is the
 * Eiffel Tower"). This tool fills that gap.
 *
 * Implementation: build a standard Android [geo URI][geo-uri] and hand
 * it to `os.intents.openUrl(...)`, which fires `Intent.ACTION_VIEW`.
 * Any installed maps app (Google Maps, Organic Maps, Waze, etc.) that
 * registers the `geo` scheme handles it.
 *
 *   geo:LAT,LNG?q=LAT,LNG(LABEL)
 *
 * The `?q=` parameter is what places the marker; without it most maps
 * apps just pan-and-zoom without dropping a pin. The `(LABEL)` suffix
 * is what the user sees on the pin — useful for "my current location"
 * vs. "the address you asked about" disambiguation.
 *
 * [geo-uri]: https://developer.android.com/guide/components/intents-common#Maps
 */
internal class ShowLocationOnMapTool(ctx: WeftContext) : WeftTool<ShowLocationOnMapTool.Args, String>(
    ctx = ctx,
    argsType = KotlinTypeToken(typeOf<Args>()),
    resultType = KotlinTypeToken(typeOf<String>()),
    descriptor = ToolDescriptor(
        // Renamed from `show_location_on_map` to `open_map` after observing
        // Claude consistently narrating "I'll open the map" without emitting
        // the tool-use block. Verb-noun names (open_map, send_email,
        // get_weather) match Anthropic's docs convention and trigger
        // selection more reliably than longer compound names.
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
    internal data class Args(
        val latitude: Double,
        val longitude: Double,
        val label: String? = null,
    )

    override suspend fun executeWeft(args: Args): String {
        // Diagnostic log — surfaces in `adb logcat -s UndercurrentMaps` so
        // you can verify whether the model is actually picking this tool
        // vs. just narrating "I'll open the map" without firing a call.
        // If you see this line, the tool fired; if you don't, the model
        // skipped it (description / catalog issue, not an execution issue).
        android.util.Log.d(
            "UndercurrentMaps",
            "open_map called: lat=${args.latitude}, lng=${args.longitude}, label=${args.label}",
        )

        // Coarse-grained validation. The substrate's openUrl will just
        // try the intent regardless, but a sanity check catches the
        // common LLM mistake of swapping lat/lng or passing wildly
        // out-of-range values, returning a useful error instead of
        // silently launching maps at the wrong place.
        if (args.latitude !in -90.0..90.0) {
            return "Invalid latitude ${args.latitude} (must be -90 to 90)."
        }
        if (args.longitude !in -180.0..180.0) {
            return "Invalid longitude ${args.longitude} (must be -180 to 180)."
        }

        val displayLabel = (args.label?.takeIf { it.isNotBlank() } ?: "Location")
            // The geo URI's label can't contain unescaped parentheses
            // or commas — those are reserved punctuation in the spec.
            // Strip them rather than url-encoding to keep the pin label
            // readable on devices that don't decode it.
            .replace(Regex("[(),]"), " ")
            .trim()

        val uri = "geo:${args.latitude},${args.longitude}" +
            "?q=${args.latitude},${args.longitude}($displayLabel)"
        val opened = os.intents.openUrl(uri, inApp = false)
        return if (opened) {
            "Opened the map at ${args.latitude}, ${args.longitude} ($displayLabel)."
        } else {
            // No maps handler installed, or the user has every maps app
            // disabled. Surface this to the agent so it can fall back
            // to reading the address aloud (or whatever).
            "Could not open the map — no maps app appears to be installed."
        }
    }
}

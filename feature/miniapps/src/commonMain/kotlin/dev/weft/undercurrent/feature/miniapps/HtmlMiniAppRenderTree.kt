package dev.weft.undercurrent.feature.miniapps

import dev.weft.contracts.ComponentNode
import dev.weft.undercurrent.core.model.MiniApp
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The render tree for a saved HTML mini-app: a single bridged `Html`
 * component carrying the mini-app's document, its [MiniApp.id] (so the
 * bridge's scope gate + state store resolve to this mini-app), and
 * `runScripts = true` so its `window.weft` bridge is live. Emitted on tap
 * in place of an agent turn — the mini-app renders instantly from cache.
 */
fun htmlMiniAppRenderTree(miniApp: MiniApp): ComponentNode = ComponentNode(
    type = "Html",
    props = buildJsonObject {
        put("html", miniApp.htmlDocument ?: "")
        put("miniAppId", miniApp.id)
        put("runScripts", true)
        // Render full-screen: the chromeless rendered-tree screen drops its
        // chrome for a single Html node, and "screen" makes the WebView fill it.
        put("height", "screen")
    },
)

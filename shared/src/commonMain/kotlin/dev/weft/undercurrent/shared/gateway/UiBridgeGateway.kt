package dev.weft.undercurrent.shared.gateway

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Observable surface for the agent's `ui_render` events. The Android
 * impl wraps Weft's `ComposeUiBridge.lastUpdate`; iOS stub emits
 * nothing (no agent to render from).
 *
 * Limited scope: only [UiRenderEvent] (component-tree rendering) flows
 * through here. Weft's older `Navigate`/`Replace`/`Patch`/`Overlay`
 * variants stay inside `:data:weft` for now — they're tied to the
 * Android ScreenSpec template registry which has no commonMain mirror.
 *
 * The [ComponentNode] mirror is wire-compatible with
 * `dev.weft.contracts.ComponentNode`: cached-render JSON written by
 * one round-trips through the other.
 */
interface UiBridgeGateway {

    /**
     * Hot flow of render events. Subscribers receive every
     * `UIUpdate.RenderTree` the agent emits. The flow is shared — late
     * subscribers see the most recent event (replay = 1) so a screen
     * that mounts mid-turn still shows what's been rendered.
     */
    val renderEvents: Flow<UiRenderEvent>
}

/**
 * One render-tree update from the agent. [agentContextId] mirrors the
 * Weft `AgentContext.contextId` and is used to scope actions back to
 * the same agent-aware screen — null for non-contextual renders.
 */
data class UiRenderEvent(
    val tree: ComponentNode,
    val agentContextId: String? = null,
)

/**
 * Mirror of `dev.weft.contracts.ComponentNode`. JSON-compatible — the
 * cached `lastRenderTreeJson` strings written by Weft deserialize
 * cleanly into this type.
 */
@Serializable
data class ComponentNode(
    val type: String,
    val props: JsonObject = JsonObject(emptyMap()),
    val children: List<ComponentNode> = emptyList(),
) {
    companion object {
        const val MAX_DEPTH: Int = 6
    }
}

fun ComponentNode.depth(): Int =
    if (children.isEmpty()) 1 else 1 + children.maxOf { it.depth() }

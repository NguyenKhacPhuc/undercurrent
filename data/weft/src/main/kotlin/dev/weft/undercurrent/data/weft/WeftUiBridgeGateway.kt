package dev.weft.undercurrent.data.weft

import androidx.compose.runtime.snapshotFlow
import dev.weft.compose.ComposeUiBridge
import dev.weft.contracts.UIUpdate
import dev.weft.undercurrent.shared.gateway.ComponentNode
import dev.weft.undercurrent.shared.gateway.UiBridgeGateway
import dev.weft.undercurrent.shared.gateway.UiRenderEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.serialization.json.Json
import dev.weft.contracts.ComponentNode as WeftComponentNode

/**
 * Android impl of [UiBridgeGateway]. Bridges Weft's [ComposeUiBridge]
 * (Compose-state `lastUpdate`) to a KMP-friendly flow.
 *
 * Only [UIUpdate.RenderTree] events pass through — overlays / legacy
 * ScreenSpec variants are filtered out (they stay handled by the
 * Android-side ComposeUiBridge directly).
 *
 * The component-tree projection re-encodes through JSON, which is the
 * cheapest correct path since the commonMain mirror and the Weft type
 * share a wire format. Trees are small (capped at [ComponentNode.MAX_DEPTH])
 * so the round-trip cost is negligible per turn.
 */
public class WeftUiBridgeGateway(
    private val bridge: ComposeUiBridge,
) : UiBridgeGateway {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val renderEvents: Flow<UiRenderEvent> =
        snapshotFlow { bridge.lastUpdate }
            .map { it.toRenderEvent() }
            .filterNotNull()
            .shareIn(scope, SharingStarted.Eagerly, replay = 1)

    private fun UIUpdate?.toRenderEvent(): UiRenderEvent? {
        val tree = this as? UIUpdate.RenderTree ?: return null
        return UiRenderEvent(
            tree = tree.tree.toCommon(),
            agentContextId = tree.agentContext?.contextId,
        )
    }

    private fun WeftComponentNode.toCommon(): ComponentNode {
        // JSON round-trip is the simplest correct mapping — both sides
        // share the same wire format. See WeftUiBridgeGateway KDoc.
        val json = JSON.encodeToString(WeftComponentNode.serializer(), this)
        return JSON.decodeFromString(ComponentNode.serializer(), json)
    }

    private companion object {
        private val JSON = Json { ignoreUnknownKeys = true }
    }
}

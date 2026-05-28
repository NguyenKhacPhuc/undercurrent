package dev.weft.undercurrent.shared.gateway

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * iOS stub. No agent → no UI render events.
 */
public class StubUiBridgeGateway : UiBridgeGateway {
    override val renderEvents: Flow<UiRenderEvent> = emptyFlow()
}

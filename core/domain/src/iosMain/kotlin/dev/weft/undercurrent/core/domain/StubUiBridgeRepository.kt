package dev.weft.undercurrent.core.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * iOS stub. No agent → no UI render events.
 */
class StubUiBridgeRepository : UiBridgeRepository {
    override val renderEvents: Flow<UiRenderEvent> = emptyFlow()
}

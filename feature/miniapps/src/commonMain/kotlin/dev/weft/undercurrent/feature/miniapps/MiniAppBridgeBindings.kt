package dev.weft.undercurrent.feature.miniapps

import dev.weft.compose.components.MiniAppScopeResolver
import dev.weft.compose.components.MiniAppStateStore
import dev.weft.undercurrent.core.domain.MiniAppsRepository
import dev.weft.undercurrent.core.model.MiniApp

/**
 * Resolve a mini-app's approved action set for the substrate scope gate:
 * the scopes the user approved, screened once more through the app's
 * offerable menu — so an action that's no longer offerable can't slip
 * through a stale grant.
 *
 * Secure by default: an unknown or unidentified mini-app resolves to the
 * empty set (gated, nothing approved), never `null` (which the bridge
 * treats as ungated). Only a mini-app present in the catalog gets its
 * approved-and-offerable scopes.
 */
fun miniAppScopeResolver(
    miniApps: () -> List<MiniApp>,
    offerable: OfferableActions,
): MiniAppScopeResolver = resolve@{ id ->
    val app = id?.let { mid -> miniApps().firstOrNull { it.id == mid } } ?: return@resolve emptySet()
    offerable.screen(app.approvedScopes).offerable
}

/**
 * A [MiniAppStateStore] backed by the [MiniAppsRepository] catalog, so a
 * mini-app's `getState`/`setState` persists in the same store as the
 * mini-app itself, isolated by id.
 */
class MiniAppRepositoryStateStore(
    private val repo: MiniAppsRepository,
) : MiniAppStateStore {
    override suspend fun get(miniAppId: String?): String? =
        miniAppId?.let { repo.getState(it) }

    override suspend fun set(miniAppId: String?, stateJson: String) {
        if (miniAppId != null) repo.setState(miniAppId, stateJson)
    }
}

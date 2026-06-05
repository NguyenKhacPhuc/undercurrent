package dev.weft.undercurrent.feature.miniapps

import dev.weft.compose.components.MiniAppActionInvoker
import dev.weft.compose.components.MiniAppStateStore
import io.ktor.client.HttpClient

/**
 * One offerable action's implementation: given the calling mini-app's
 * [miniAppId] and the call's `args` JSON, do the work and return a JSON
 * result string. Throwing surfaces to the mini-app as a rejected Promise
 * (the bridge forwards the message).
 */
fun interface MiniAppActionHandler {
    suspend fun handle(miniAppId: String?, argsJson: String): String
}

/**
 * Routes a mini-app's action call to its [handlers], screened once more
 * through the app's offerable menu. A name that isn't offerable — or has
 * no registered handler — resolves to `null`, which the bridge surfaces
 * as "no such action": a non-offerable action is indistinguishable from
 * one that doesn't exist. A handler that throws propagates so the bridge
 * rejects with its message.
 *
 * The substrate bridge already gates by the approved set, so this
 * screening is defense in depth — it also makes the invoker safe to use
 * on an ungated bridge.
 */
class RoutingMiniAppActionInvoker(
    private val offerable: OfferableActions,
    private val handlers: Map<String, MiniAppActionHandler>,
) : MiniAppActionInvoker {
    override suspend fun invoke(miniAppId: String?, name: String, argsJson: String): String? {
        if (!offerable.isOfferable(name)) return null
        val handler = handlers[name] ?: return null
        return handler.handle(miniAppId, argsJson)
    }
}

/**
 * Assemble the single invoker the bridged HTML mini-app runtime calls,
 * wiring the v1 offerable set ([OfferableActions.readMostlyDefaults]) to
 * real behavior: `store_get` / `store_set` over [stateStore] (now keyed
 * by the per-call `miniAppId` the substrate threads through), and
 * `http_fetch` over [httpClient] (the host installs its NetworkPolicy
 * allowlist plugin on that client).
 *
 * One instance serves every mini-app: the id arrives per call, so a
 * single registered `HtmlComponent` routes each mini-app's storage to its
 * own slot.
 */
fun miniAppActionInvoker(
    offerable: OfferableActions,
    stateStore: MiniAppStateStore,
    httpClient: HttpClient,
): MiniAppActionInvoker = RoutingMiniAppActionInvoker(
    offerable,
    mapOf(
        "store_get" to storeGetHandler(stateStore),
        "store_set" to storeSetHandler(stateStore),
        "http_fetch" to httpFetchHandler(httpClient),
    ),
)

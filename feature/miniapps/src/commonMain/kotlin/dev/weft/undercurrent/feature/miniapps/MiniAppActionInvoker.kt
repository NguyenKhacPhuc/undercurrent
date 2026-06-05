package dev.weft.undercurrent.feature.miniapps

import dev.weft.compose.components.MiniAppActionInvoker
import dev.weft.compose.components.MiniAppStateStore
import io.ktor.client.HttpClient

/**
 * One offerable action's implementation: given the call's `args` JSON,
 * do the work and return a JSON result string. Throwing surfaces to the
 * mini-app as a rejected Promise (the bridge forwards the message).
 */
fun interface MiniAppActionHandler {
    suspend fun handle(argsJson: String): String
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
    override suspend fun invoke(name: String, argsJson: String): String? {
        if (!offerable.isOfferable(name)) return null
        val handler = handlers[name] ?: return null
        return handler.handle(argsJson)
    }
}

/**
 * The invoker a singleton bridged `HtmlComponent` calls — wires only the
 * mini-app-id-independent actions, since the substrate's
 * `MiniAppActionInvoker.invoke(name, args)` carries no mini-app id and
 * one component instance renders every mini-app. Today that's
 * `http_fetch` over [httpClient] (the host installs its NetworkPolicy
 * allowlist plugin on that client).
 *
 * `store_get` / `store_set` are intentionally absent: keyed by mini-app,
 * they can't be routed correctly here until the substrate threads the id
 * through `invoke`. Mini-apps persist via the bridge's `getState` /
 * `setState` (the host [MiniAppStateStore]) meanwhile. See
 * [miniAppActionInvoker] for the per-mini-app variant they belong to.
 */
fun miniAppHttpInvoker(
    offerable: OfferableActions,
    httpClient: HttpClient,
): MiniAppActionInvoker = RoutingMiniAppActionInvoker(
    offerable,
    mapOf("http_fetch" to httpFetchHandler(httpClient)),
)

/**
 * Assemble the invoker the bridged HTML mini-app runtime calls, wiring
 * the v1 offerable set ([OfferableActions.readMostlyDefaults]) to real
 * behavior: `store_get` / `store_set` over [stateStore] keyed by
 * [miniAppId], and `http_fetch` over [httpClient] (the host installs its
 * NetworkPolicy allowlist plugin on that client). One invoker per
 * mini-app, since the store handlers are bound to a single [miniAppId].
 *
 * Not yet wired into the live bridge: the substrate registers a single
 * `HtmlComponent` for all mini-apps and `invoke` has no id, so the
 * singleton path uses [miniAppHttpInvoker] until `invoke` carries one.
 */
fun miniAppActionInvoker(
    offerable: OfferableActions,
    stateStore: MiniAppStateStore,
    httpClient: HttpClient,
    miniAppId: String?,
): MiniAppActionInvoker = RoutingMiniAppActionInvoker(
    offerable,
    mapOf(
        "store_get" to storeGetHandler(stateStore, miniAppId),
        "store_set" to storeSetHandler(stateStore, miniAppId),
        "http_fetch" to httpFetchHandler(httpClient),
    ),
)

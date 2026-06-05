package dev.weft.undercurrent.feature.miniapps

import dev.weft.compose.components.MiniAppActionInvoker

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

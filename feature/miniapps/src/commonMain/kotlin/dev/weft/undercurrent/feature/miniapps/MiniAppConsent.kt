package dev.weft.undercurrent.feature.miniapps

import dev.weft.undercurrent.core.model.ConsentAction
import dev.weft.undercurrent.core.model.MiniApp
import dev.weft.undercurrent.core.model.MiniAppConsentRequest

/**
 * True when this is a flexible HTML mini-app that declares actions and
 * the user hasn't yet decided its grant — i.e. show the first-run consent
 * prompt before it renders. A decided grant (even an empty one) doesn't
 * re-prompt; a mini-app that declares nothing has nothing to consent to.
 */
fun MiniApp.needsConsent(): Boolean =
    htmlDocument != null && consentedAt == null && declaredScopes.isNotEmpty()

/**
 * The actions to show in this mini-app's consent prompt: its declared
 * scopes screened to the app's [offerable] menu (an action the app
 * doesn't offer never reaches the user), each with its description for a
 * plain-language list. Preserves the declared order.
 */
fun MiniApp.requestedActions(offerable: OfferableActions): List<AppAction> {
    val screened = offerable.screen(declaredScopes).offerable
    val byName = offerable.available.associateBy { it.name }
    return screened.mapNotNull { byName[it] }
}

/**
 * The scope set to record when the user approves this mini-app: exactly
 * its declared-and-offerable actions. (Denial records the empty set.)
 */
fun MiniApp.approvableScopes(offerable: OfferableActions): Set<String> =
    offerable.screen(declaredScopes).offerable

/** Build the consent prompt this mini-app shows on first run. */
fun MiniApp.toConsentRequest(offerable: OfferableActions): MiniAppConsentRequest =
    MiniAppConsentRequest(
        miniAppId = id,
        miniAppName = name,
        miniAppEmoji = emoji,
        actions = requestedActions(offerable).map { ConsentAction(it.name, it.description) },
    )

package dev.weft.undercurrent.feature.miniapps

/**
 * An action the app is ever willing to offer to a mini-app. The app
 * maintainer — not the agent, not the mini-app — owns this menu, so a
 * mini-app can never request something the app hasn't sanctioned.
 *
 * v1 is deliberately read-mostly (html-mini-apps open-question Q1):
 * allowlisted HTTP, the mini-app key-value store, share, clipboard
 * read, and system info. Destructive / sensitive actions (deleting
 * data, sending messages, spending, location) are excluded until a
 * stronger per-use consent model exists.
 */
enum class OfferableAction(val actionName: String) {
    HttpFetch("http_fetch"),
    StoreGet("store_get"),
    StoreSet("store_set"),
    Share("share"),
    ClipboardRead("clipboard_read"),
    SystemInfo("system_info"),
}

/**
 * The app's offerable-action allowlist and the screening that keeps a
 * mini-app's requested scopes within it. The substrate enforces the
 * approved set per call ([[03-scope-gate]]); this is the policy layer
 * above it — only an offerable action can ever reach the user's
 * approval prompt.
 */
object OfferableActions {

    val all: Set<OfferableAction> = OfferableAction.entries.toSet()

    /** Offerable action names — the universe a mini-app may request from. */
    val names: Set<String> = all.mapTo(LinkedHashSet()) { it.actionName }

    fun isOfferable(actionName: String): Boolean = actionName in names

    /**
     * Split a mini-app's requested action names into those that may
     * proceed to approval (offerable) and those rejected outright
     * because they aren't on the app's menu.
     */
    fun screen(requested: Set<String>): OfferableScreening {
        val offerable = requested.filterTo(LinkedHashSet()) { it in names }
        val rejected = requested.filterTo(LinkedHashSet()) { it !in names }
        return OfferableScreening(offerable = offerable, rejected = rejected)
    }
}

/**
 * Outcome of screening a mini-app's requested scopes against the
 * offerable menu: [offerable] may go to the user for approval;
 * [rejected] is refused before the user ever sees it.
 */
data class OfferableScreening(
    val offerable: Set<String>,
    val rejected: Set<String>,
)

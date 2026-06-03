package dev.weft.undercurrent.feature.miniapps

/**
 * An action the app exposes. [offerable] marks whether a mini-app may
 * ever request it: the app registers every action it has and flags the
 * ones that are safe to offer. Defaults to offerable; mark sensitive /
 * destructive actions `offerable = false` to keep them app-only.
 */
data class AppAction(
    val name: String,
    val description: String = "",
    val offerable: Boolean = true,
)

/**
 * The app's registry of actions and the screening that keeps a
 * mini-app's requested scopes within the *offerable* subset.
 *
 * The app registers whatever actions it has — this is an open registry,
 * not a fixed list, so new app actions are added by registering them
 * (see [plus]), not by editing an enum. Only actions flagged
 * [AppAction.offerable] can ever reach a mini-app's approval prompt, so
 * registering an action never by itself exposes it. The substrate
 * enforces the approved set per call (`03-scope-gate`); this is the
 * policy layer above it.
 */
class OfferableActions(actions: Iterable<AppAction>) {

    private val byName: Map<String, AppAction> = actions.associateBy { it.name }

    /** Every registered action, offerable or not. */
    val available: Set<AppAction> = byName.values.toSet()

    /** Names of the subset a mini-app may request from. */
    val offerableNames: Set<String> =
        byName.values.filter { it.offerable }.mapTo(LinkedHashSet()) { it.name }

    fun isOfferable(actionName: String): Boolean = byName[actionName]?.offerable == true

    /**
     * Split a mini-app's requested action names into those that may
     * proceed to approval (registered and offerable) and those rejected
     * outright — unregistered or app-only — before the user is asked.
     */
    fun screen(requested: Set<String>): OfferableScreening {
        val offerable = requested.filterTo(LinkedHashSet()) { isOfferable(it) }
        val rejected = requested.filterTo(LinkedHashSet()) { !isOfferable(it) }
        return OfferableScreening(offerable = offerable, rejected = rejected)
    }

    /** Register more actions on top of these (later names override earlier). */
    operator fun plus(more: Iterable<AppAction>): OfferableActions =
        OfferableActions(available + more)

    companion object {
        /**
         * The read-mostly v1 offerable set (html-mini-apps Q1):
         * allowlisted HTTP, the mini-app key-value store, share,
         * clipboard read, system info. The app registers anything else
         * it offers via [plus]; destructive / sensitive actions register
         * with `offerable = false`.
         */
        fun readMostlyDefaults(): OfferableActions = OfferableActions(
            listOf(
                AppAction("http_fetch", "Fetch from an allowlisted URL"),
                AppAction("store_get", "Read the mini-app's key-value store"),
                AppAction("store_set", "Write the mini-app's key-value store"),
                AppAction("share", "Share content via the system share sheet"),
                AppAction("clipboard_read", "Read the clipboard"),
                AppAction("system_info", "Read basic device / system info"),
            ),
        )
    }
}

/**
 * Outcome of screening a mini-app's requested scopes: [offerable] may go
 * to the user for approval; [rejected] is refused before the user is
 * ever asked.
 */
data class OfferableScreening(
    val offerable: Set<String>,
    val rejected: Set<String>,
)

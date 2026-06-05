package dev.weft.undercurrent.data.weft.tools

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.serialization.KotlinTypeToken
import dev.weft.tools.WeftContext
import dev.weft.tools.WeftTool
import dev.weft.undercurrent.core.model.PersonaKind
import dev.weft.undercurrent.core.navigation.NavigationChannel
import dev.weft.undercurrent.core.navigation.Screen
import dev.weft.undercurrent.core.domain.MiniAppsRepository
import dev.weft.undercurrent.core.domain.PersonaRepository
import dev.weft.undercurrent.feature.creator.CreatorSession
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

/**
 * Finalizes a guided persona-creation QnA flow. The agent calls this
 * AFTER walking the user through ~4-6 questions (rendered via
 * `ui_render`), once it has enough material to write a coherent
 * system-prompt fragment. The tool:
 *
 *   1. Persists the new persona via [PersonaRepository.addCustom].
 *   2. Sets it active in the appropriate slot (voice / role).
 *   3. Clears the [CreatorSession].
 *   4. Navigates back to the Personas screen.
 *
 * KMP — Android-only. Moved from
 * `app/.../features/creator/CreatorTools.kt` to `:data:weft` because
 * Weft tools must live in androidMain. Now consumes the migrated
 * commonMain repositories + the migrated navigation channel.
 */
class CreatePersonaTool(
    ctx: WeftContext,
    private val personaRepo: PersonaRepository,
    private val nav: NavigationChannel,
    private val creatorSession: CreatorSession,
) : WeftTool<CreatePersonaTool.Args, String>(
    ctx = ctx,
    argsType = KotlinTypeToken(typeOf<Args>()),
    resultType = KotlinTypeToken(typeOf<String>()),
    descriptor = ToolDescriptor(
        name = "create_persona",
        description = "Finalize a new custom persona after gathering details from the user. " +
            "Call ONLY after you've walked the user through enough questions to write a " +
            "good system prompt (name, what they're good at, tone, any constraints). " +
            "name: short label (e.g. 'Code reviewer'). tagline: one-liner shown in the picker. " +
            "systemPrompt: the actual instructions the model will receive when this persona is " +
            "active — 3-6 sentences, action-leading, concrete. kind: 'voice' (writing style) " +
            "or 'role' (professional expertise). Sets the new persona active automatically.",
        requiredParameters = listOf(
            ToolParameterDescriptor("name", "Short display name, e.g. 'Code reviewer'.", ToolParameterType.String),
            ToolParameterDescriptor("tagline", "One-line summary for the picker.", ToolParameterType.String),
            ToolParameterDescriptor(
                "systemPrompt",
                "The actual model instructions — 3-6 sentences, concrete, action-leading.",
                ToolParameterType.String,
            ),
            ToolParameterDescriptor(
                "kind",
                "'voice' (writing style — Editor, Field Notes…) or 'role' (expertise — Doctor, Developer…).",
                ToolParameterType.String,
            ),
        ),
        optionalParameters = emptyList(),
    ),
    sideEffecting = true,
) {

    @Serializable
    data class Args(
        val name: String,
        val tagline: String,
        val systemPrompt: String,
        val kind: String,
    )

    override suspend fun executeWeft(args: Args): String {
        val personaKind = when (args.kind.lowercase().trim()) {
            "voice" -> PersonaKind.Voice
            "role" -> PersonaKind.Role
            else -> PersonaKind.Custom
        }
        val created = personaRepo.addCustom(
            name = args.name.trim(),
            tagline = args.tagline.trim(),
            systemPromptText = args.systemPrompt.trim(),
            kind = personaKind,
        )
        when (personaKind) {
            PersonaKind.Voice, PersonaKind.Custom -> personaRepo.setActiveVoice(created.id)
            PersonaKind.Role -> personaRepo.setActiveRole(created.id)
        }
        creatorSession.clear()
        nav.requestNavigate(Screen.Personas)
        return "Created persona '${created.name}' (${personaKind.name.lowercase()}). " +
            "It's now active — your next reply will use it."
    }
}

/**
 * Finalizes a guided mini-app-creation QnA flow. Persists the new
 * mini-app via [MiniAppsRepository.add], clears the [CreatorSession],
 * and navigates to the Mini Apps screen.
 */
class CreateMiniAppTool(
    ctx: WeftContext,
    private val miniAppsRepo: MiniAppsRepository,
    private val nav: NavigationChannel,
    private val creatorSession: CreatorSession,
) : WeftTool<CreateMiniAppTool.Args, String>(
    ctx = ctx,
    argsType = KotlinTypeToken(typeOf<Args>()),
    resultType = KotlinTypeToken(typeOf<String>()),
    descriptor = ToolDescriptor(
        name = "create_mini_app",
        description = "Finalize a new mini-app after gathering details from the user. " +
            "A mini-app is a shortcut: a name + emoji + trigger prompt that re-runs the " +
            "same kind of UI/task on demand. Call ONLY after you've collected: name, emoji, " +
            "and a clear trigger prompt (the user message that should be re-dispatched when " +
            "they invoke the mini-app). Don't invent these — ask the user.",
        requiredParameters = listOf(
            ToolParameterDescriptor("name", "Short display name, e.g. 'Log water'.", ToolParameterType.String),
            ToolParameterDescriptor("emoji", "Single emoji used as the card icon, e.g. '💧'.", ToolParameterType.String),
            ToolParameterDescriptor(
                "triggerPrompt",
                "The user-message text that should be dispatched when the user invokes the mini-app.",
                ToolParameterType.String,
            ),
        ),
        optionalParameters = emptyList(),
    ),
    sideEffecting = true,
) {

    @Serializable
    data class Args(
        val name: String,
        val emoji: String,
        val triggerPrompt: String,
    )

    override suspend fun executeWeft(args: Args): String {
        val created = miniAppsRepo.add(
            name = args.name.trim(),
            emoji = args.emoji.trim().ifBlank { "✨" },
            triggerPrompt = args.triggerPrompt.trim(),
        )
        creatorSession.clear()
        nav.requestNavigate(Screen.MiniApps)
        return "Created mini-app '${created.name}'. " +
            "Tap it on the Mini Apps screen to run it the first time — its UI gets cached automatically."
    }
}

/**
 * Saves a *flexible (HTML) mini-app* the agent has authored: a
 * self-contained HTML document plus the device/app actions it needs.
 * Unlike [CreateMiniAppTool] (which saves a trigger prompt that re-runs
 * the agent), this saves the document itself — rendered instantly on tap
 * via the bridged Html component, with its `window.weft` bridge live and
 * scope-gated to what the user approves on first run.
 *
 * KMP — Android-only (lives in `:data:weft`).
 */
class CreateHtmlMiniAppTool(
    ctx: WeftContext,
    private val miniAppsRepo: MiniAppsRepository,
    private val nav: NavigationChannel,
) : WeftTool<CreateHtmlMiniAppTool.Args, String>(
    ctx = ctx,
    argsType = KotlinTypeToken(typeOf<Args>()),
    resultType = KotlinTypeToken(typeOf<String>()),
    descriptor = ToolDescriptor(
        name = "create_html_mini_app",
        description = "Save a self-contained interactive HTML mini-app the user can reopen with one tap. " +
            "Use for bespoke widgets the palette can't express — a calculator, tracker, small game, " +
            "custom tool. Keep the document focused so it fits in one response. " +
            "USE THE window.weft BRIDGE, not standard web APIs: " +
            "(1) fetch()/XMLHttpRequest are BLOCKED — for network do " +
            "`await window.weft.callTool('http_fetch', { url })`, which resolves to { status, body } " +
            "where body is the response TEXT (JSON.parse(result.body) for JSON APIs). " +
            "(2) Persist with `await window.weft.setState(obj)` / `await window.weft.getState()` " +
            "(resolves to the saved object or null). " +
            "(3) Ask the assistant with `await window.weft.sendMessage(text)` (resolves to the reply string). " +
            "Remote https images/CSS/fonts/media and https iframes are allowed; remote <script src> is NOT " +
            "(inline JS only). Wrap bridge calls in try/catch and show errors on screen. " +
            "Declare in scopes ONLY the callTool actions it uses, from: http_fetch, store_get, store_set " +
            "(approved on first run; getState/setState/sendMessage need no scope).",
        requiredParameters = listOf(
            ToolParameterDescriptor("name", "Short display name, e.g. 'Tip Calculator'.", ToolParameterType.String),
            ToolParameterDescriptor("emoji", "Single emoji used as the card icon, e.g. '🧮'.", ToolParameterType.String),
            ToolParameterDescriptor(
                "html",
                "The complete self-contained HTML document. Inline JS/CSS (no remote <script>); " +
                    "remote https images/css/fonts/iframes are fine. For network/state/assistant use " +
                    "the window.weft bridge (see tool description), never fetch().",
                ToolParameterType.String,
            ),
        ),
        optionalParameters = listOf(
            ToolParameterDescriptor(
                "scopes",
                "Action names the mini-app needs, from the offerable set. Omit/empty if it needs none.",
                ToolParameterType.List(itemsType = ToolParameterType.String),
            ),
        ),
    ),
    sideEffecting = true,
) {

    @Serializable
    data class Args(
        val name: String,
        val emoji: String,
        val html: String,
        val scopes: List<String> = emptyList(),
    )

    override suspend fun executeWeft(args: Args): String {
        val created = miniAppsRepo.addHtml(
            name = args.name.trim(),
            emoji = args.emoji.trim().ifBlank { "✨" },
            html = args.html,
            declaredScopes = args.scopes.map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
        )
        nav.requestNavigate(Screen.MiniApps)
        return "Created HTML mini-app '${created.name}'. " +
            "Tap it on the Mini Apps screen to open it — it renders instantly, and the first time it " +
            "uses an action the user is asked to approve it."
    }
}

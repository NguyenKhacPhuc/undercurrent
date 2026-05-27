package dev.weft.undercurrent.features.creator

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.serialization.KotlinTypeToken
import dev.weft.tools.WeftContext
import dev.weft.tools.WeftTool
import dev.weft.undercurrent.core.Screen
import dev.weft.undercurrent.features.miniapps.MiniAppsRepository
import dev.weft.undercurrent.features.navigation.NavigationChannel
import dev.weft.undercurrent.features.personas.PersonaKind
import dev.weft.undercurrent.features.personas.PersonaRepository
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

/**
 * Finalizes a guided persona-creation QnA flow.
 *
 * The agent calls this AFTER walking the user through ~4-6 questions
 * (rendered via `ui_render`), once it has enough material to write a
 * coherent system-prompt fragment. The tool:
 *
 *   1. Persists the new persona via [PersonaRepository.addCustom].
 *   2. Sets it active in the appropriate slot (voice slot for Voice,
 *      role slot for Role) so the next turn picks it up.
 *   3. Clears the [CreatorSession] (so the creator-mode preamble stops
 *      injecting on subsequent turns).
 *   4. Navigates back to the Personas screen.
 *
 * Available outside creator screens too — a user could chat "make me a
 * persona that's good at code reviews" and the agent would gather the
 * details conversationally then call this tool.
 */
internal class CreatePersonaTool(
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
        // Activate in the slot that matches the kind so the next reply
        // reflects the new persona's voice.
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
 * Finalizes a guided mini-app-creation QnA flow.
 *
 * The agent calls this AFTER walking the user through what they want to
 * build (name, the trigger prompt, an emoji). The tool persists the new
 * mini-app via [MiniAppsRepository.add], clears the [CreatorSession],
 * and navigates to the Mini Apps screen so the user sees it land.
 *
 * Note: the actual UI tree for the mini-app gets cached on first
 * invocation (handled by AppStore). Creation here just registers the
 * shortcut.
 */
internal class CreateMiniAppTool(
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

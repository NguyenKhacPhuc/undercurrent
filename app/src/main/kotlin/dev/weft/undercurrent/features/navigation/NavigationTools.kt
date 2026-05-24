package dev.weft.undercurrent.features.navigation

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.serialization.KotlinTypeToken
import dev.weft.tools.WeftContext
import dev.weft.tools.WeftTool
import dev.weft.undercurrent.core.Screen
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

/**
 * Agent-facing navigation tools.
 *
 * Each tool maps to one in-app destination. When the LLM hears something
 * like "I want to switch personas," "show me what you remember," or
 * "let me manage integrations" — it picks the right tool, which fires a
 * [NavigationChannel] event. [dev.weft.undercurrent.core.AppStore]
 * collects from the channel and dispatches `AppIntent.Navigate`, so
 * navigation goes through the same reducer path as a UI tap. That keeps
 * back-button routing (`AppState.previousScreen`) accurate even when
 * the entry point is an agent tool.
 *
 * **Why per-destination tools instead of one parameterized `navigate`
 * tool?** Discoverability. The LLM picks tools by reading descriptions;
 * five focused tools with concrete examples ("change voice", "show
 * memories") guide the model far better than one generic
 * `navigate(screen)` whose description has to enumerate every option.
 * The cost is a slightly larger tool catalog — a worthwhile trade.
 *
 * **Why not directly mutate persona / theme state via these tools?**
 * Some surfaces (Theme) do mutate directly via dedicated tools
 * (`set_theme_palette`, `set_theme_mode`) because the parameter space
 * is tiny and well-typed. Personas / Integrations require *user choice*
 * (which voice? which connector?), so the right UX is to put the user
 * in front of the picker rather than guess.
 */

private val argsType = KotlinTypeToken(typeOf<NoArgs>())
private val resultType = KotlinTypeToken(typeOf<String>())

/**
 * Shared "no real args" placeholder. Anthropic's tool-use API rejects
 * schemas with zero properties, so we surface a token `context` field
 * the model can fill with a free-form reason ("user asked to switch
 * voices"). The value is ignored at execution time — purely there to
 * satisfy the schema and to give the trace a human-readable hint.
 */
@Serializable
internal data class NoArgs(val context: String = "")

private val contextParam = ToolParameterDescriptor(
    "context",
    "Why you're navigating, e.g. 'user wants to change persona'. Any short string; ignored.",
    ToolParameterType.String,
)

internal class OpenPersonasTool(
    ctx: WeftContext,
    private val nav: NavigationChannel,
) : WeftTool<NoArgs, String>(
    ctx = ctx,
    argsType = argsType,
    resultType = resultType,
    descriptor = ToolDescriptor(
        name = "open_personas",
        description = "Open the Personas screen so the user can pick a voice " +
            "(Editor, Field Notes, Reader, Almanac, Default) or a role " +
            "(Developer, Doctor, Lawyer, Teacher, Researcher), or create a " +
            "custom one. Use when the user asks to change persona / voice / " +
            "writing style / role, e.g. 'switch to the editor voice', 'I want " +
            "you to be a doctor', 'pick a different persona', 'become someone else'.",
        requiredParameters = listOf(contextParam),
        optionalParameters = emptyList(),
    ),
    sideEffecting = true,
) {
    override suspend fun executeWeft(args: NoArgs): String {
        nav.requestNavigate(Screen.Personas)
        return "Opened the Personas screen."
    }
}

internal class OpenIntegrationsTool(
    ctx: WeftContext,
    private val nav: NavigationChannel,
) : WeftTool<NoArgs, String>(
    ctx = ctx,
    argsType = argsType,
    resultType = resultType,
    descriptor = ToolDescriptor(
        name = "open_integrations",
        description = "Open the Integrations screen so the user can connect or " +
            "disconnect third-party services (Linear, etc.) via OAuth. Use when " +
            "the user asks to connect / disconnect an integration, manage " +
            "connectors, or wire up an external service — e.g. 'connect Linear', " +
            "'set up integrations', 'remove the Linear connection'.",
        requiredParameters = listOf(contextParam),
        optionalParameters = emptyList(),
    ),
    sideEffecting = true,
) {
    override suspend fun executeWeft(args: NoArgs): String {
        nav.requestNavigate(Screen.Integrations)
        return "Opened the Integrations screen."
    }
}

internal class OpenMemoriesTool(
    ctx: WeftContext,
    private val nav: NavigationChannel,
) : WeftTool<NoArgs, String>(
    ctx = ctx,
    argsType = argsType,
    resultType = resultType,
    descriptor = ToolDescriptor(
        name = "open_memories",
        description = "Open the Memories screen so the user can review or delete " +
            "facts the assistant has stored about them. Use when the user asks " +
            "to see / edit / wipe memories, e.g. 'what do you remember about me', " +
            "'show my memories', 'forget that I said X', 'clear what you know'.",
        requiredParameters = listOf(contextParam),
        optionalParameters = emptyList(),
    ),
    sideEffecting = true,
) {
    override suspend fun executeWeft(args: NoArgs): String {
        nav.requestNavigate(Screen.Memories)
        return "Opened the Memories screen."
    }
}

internal class OpenConversationsTool(
    ctx: WeftContext,
    private val nav: NavigationChannel,
) : WeftTool<NoArgs, String>(
    ctx = ctx,
    argsType = argsType,
    resultType = resultType,
    descriptor = ToolDescriptor(
        name = "open_conversations",
        description = "Open the Conversations list so the user can browse, search, " +
            "or jump to a previous chat thread. Use when the user asks to find a " +
            "past conversation, switch threads, or browse chat history, e.g. " +
            "'show my chats', 'where's that conversation about X', 'switch threads'.",
        requiredParameters = listOf(contextParam),
        optionalParameters = emptyList(),
    ),
    sideEffecting = true,
) {
    override suspend fun executeWeft(args: NoArgs): String {
        nav.requestNavigate(Screen.Conversations)
        return "Opened the Conversations list."
    }
}

internal class OpenUsageTool(
    ctx: WeftContext,
    private val nav: NavigationChannel,
) : WeftTool<NoArgs, String>(
    ctx = ctx,
    argsType = argsType,
    resultType = resultType,
    descriptor = ToolDescriptor(
        name = "open_usage",
        description = "Open the Usage screen so the user can see token + dollar " +
            "spend per provider and per model. Use when the user asks about " +
            "cost, spending, token counts, or usage history — e.g. 'how much " +
            "have I spent', 'show usage', 'what are my token counts'.",
        requiredParameters = listOf(contextParam),
        optionalParameters = emptyList(),
    ),
    sideEffecting = true,
) {
    override suspend fun executeWeft(args: NoArgs): String {
        nav.requestNavigate(Screen.Usage)
        return "Opened the Usage screen."
    }
}

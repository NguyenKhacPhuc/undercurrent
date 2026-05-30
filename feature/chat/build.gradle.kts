// Feature module — KMP + Compose Multiplatform + Koin.
// Automatically depends on :core:model, :core:ui, :core:design-system,
// :core:navigation, :core:resources via the feature convention plugin.
//
// androidMain owns the substrate-backed chat data + agent host
// (ChatRepository impl, AgentSession, AgentSlot, WeftAgentFactory)
// + the Android-only chat-route shell. iosMain will own the iOS
// chat data path (Ktor LlmClient family) + iOS chat-route shell.

plugins {
    alias(libs.plugins.undercurrent.kmp.feature)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // SpeechGateway + VoiceState.
            implementation(projects.shared)
            // ChatRepository + ChatMessage + ChatChunk + AgentSummary
            // — the chat-domain surface ChatViewModel consumes.
            implementation(projects.core.domain)
            // MiniAppIntent + MiniAppViewModel (dispatched from chat
            // shells) + SaveAsMiniAppDialog (rendered inline in
            // ChatScreen).
            implementation(projects.feature.miniapps)
            // ThemeViewModel + ThemeIntent — chat shells include the
            // theme palette/mode toggles in the Add-to-Chat sheet.
            implementation(projects.feature.theme)
            // WaveformBars (in-input audio level visualization).
            implementation(projects.feature.voice)
        }
        androidMain.dependencies {
            // Substrate — chat slice owns the Android agent host
            // (AgentSession + AgentSlot + WeftAgentFactory) plus the
            // ChatRepository impl that wraps WeftAgent.
            implementation("dev.weft:weft-runtime")
            implementation("dev.weft:weft-harness-agents")
            implementation("dev.weft:weft-harness-conversation")
            implementation("dev.weft:weft-contracts")

            // ChatRoute (Android shell) needs the drawer + permission
            // launchers + Material3.
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.compose.material3)
            implementation(libs.androidx.compose.material.icons.extended)
        }
        iosMain.dependencies {
            // Substrate — chat slice's iOS agent path (WeftAgentLlmClient
            // + IosWeftAgentFactory) goes through WeftAgent + Koog.
            implementation("dev.weft:weft-harness-agents")
            implementation("dev.weft:weft-harness-prompt")
            implementation("dev.weft:weft-harness-observability")
            implementation("dev.weft:weft-harness-conversation")
            implementation(libs.koog.agents)

            // Ktor — the parallel LlmClient family for non-Anthropic
            // providers (OpenAI / OpenRouter / DeepSeek), kept while
            // the Koog substrate path doesn't yet cover them.
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.darwin)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            // SQLDelight UndercurrentDatabase — IosChatRepository
            // persists messages here.
            implementation(projects.data.sqldelight)
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.feature.chat"
}

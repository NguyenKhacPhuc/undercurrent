// Feature module — KMP + Compose Multiplatform + Koin.
//
// androidMain owns the substrate-backed chat data + agent host
// (ChatRepository impl, AgentSession, AgentSlot, WeftAgentFactory)
// + the Android chat-route shell. iosMain is empty — iOS uses a
// StubChatRepository (in :core:domain/iosMain) so the iOS app
// boots through Onboarding/KeyPaste and the chat screen redirects
// back via the agentReady=false gate. iOS chat reactivates once
// the substrate (Weft) is fully exercised on iOS.

plugins {
    alias(libs.plugins.undercurrent.kmp.feature)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)
            implementation(projects.core.domain)
            implementation(projects.feature.miniapps)
            implementation(projects.feature.theme)
            implementation(projects.feature.voice)
        }
        androidMain.dependencies {
            implementation("dev.weft:weft-runtime")
            implementation("dev.weft:weft-harness-agents")
            implementation("dev.weft:weft-harness-conversation")
            implementation("dev.weft:weft-contracts")

            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.compose.material3)
            implementation(libs.androidx.compose.material.icons.extended)
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.feature.chat"
}

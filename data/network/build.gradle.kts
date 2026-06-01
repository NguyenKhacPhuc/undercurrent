// KMP Ktor-HTTP infrastructure for REST API integrations.
//
// What lives here:
//   - PlatformHttpClientEngineFactory + Android (OkHttp) / iOS (Darwin)
//     implementations via Koin.
//   - A configured HttpClient (retry / timeout / content-negotiation /
//     logging / error-validator).
//   - Token-refresh interceptor + TokenStore interface — the consumer
//     supplies a TokenStore impl (backed by DataStore, Keychain, …) and
//     a refreshTokenCall lambda.
//   - Generic envelope types (BaseResponse / BaseErrorResponse), error
//     mapping (ApiException / HttpException / NetworkException),
//     CustomHeader name constants.
//
// What does NOT live here:
//   - LLM-streaming (Anthropic / OpenAI-compat) — those live in
//     composeApp/iosMain because they speak SSE, not REST/JSON-envelope.
//   - App-specific service data sources — each feature module that
//     calls a REST endpoint should define its own ServiceDataSource
//     using the HttpClient bound here.

plugins {
    alias(libs.plugins.undercurrent.kmp.library)
    alias(libs.plugins.undercurrent.kmp.koin)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // AuthClient interface + DTOs + SessionTokenStore (mobile-auth-wiring/04).
            api(projects.core.domain)
            // Ktor multiplatform HTTP client.
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.resources)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            // Logging — already wired by the iOS app entrypoint (InitKoin).
            implementation(libs.napier)
        }
        commonTest.dependencies {
            // MockEngine for HttpAuthClient tests (mobile-auth-wiring/04).
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "dev.weft.undercurrent.data.network"
}

package dev.weft.undercurrent.data.network

import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.CertificatePinner
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.net.URI

/**
 * Android-side bindings for [networkModule]. Supplies:
 *
 *   - [PlatformHttpClientEngineFactory] backed by Ktor's OkHttp engine.
 *     Optionally pins certificates if the consumer registers a
 *     `List<String>(named(SSL_PINS_QUALIFIER))` of base64 SHA-256 pins.
 *
 * Consumer must register before loading:
 *   - `single<String>(named(BASE_URL_QUALIFIER)) { … }`
 *   - optionally `single<List<String>>(named(SSL_PINS_QUALIFIER)) { … }`
 *
 * NOTE — Chucker (the OkHttp debug interceptor) is NOT wired here.
 * Add it in the host app's DI if you want the in-app HTTP inspector.
 */
val androidNetworkModule = module {

    single<PlatformHttpClientEngineFactory> {
        // Cert pinning is opt-in via BASE_URL_QUALIFIER + SSL_PINS_QUALIFIER.
        // Both optional so callers that don't pin (auth HttpClient,
        // future no-pin REST clients) don't crash when their host app
        // doesn't register either qualifier.
        val baseUrl = getOrNull<String>(named(BASE_URL_QUALIFIER))
        val sslPins = getOrNull<List<String>>(named(SSL_PINS_QUALIFIER)).orEmpty().map { pin ->
            "sha256/$pin"
        }
        val host = baseUrl?.let { runCatching { URI(it).host }.getOrNull() }

        PlatformHttpClientEngineFactory {
            OkHttp.create {
                if (host != null && sslPins.isNotEmpty()) {
                    config {
                        certificatePinner(
                            CertificatePinner.Builder()
                                .add(host, *sslPins.toTypedArray())
                                .build(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Koin qualifier for the optional `List<String>` of base64-encoded
 * SHA-256 pins applied to [BASE_URL_QUALIFIER]'s host. Empty list ==
 * pinning disabled (typical for debug builds).
 */
const val SSL_PINS_QUALIFIER: String = "network.sslPins"

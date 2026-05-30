package dev.weft.undercurrent.data.network

import io.ktor.client.engine.darwin.Darwin
import org.koin.dsl.module

/**
 * iOS-side bindings for [networkModule]. Supplies:
 *
 *   - [PlatformHttpClientEngineFactory] backed by Ktor's Darwin engine
 *     (NSURLSession under the hood).
 *
 * Certificate pinning on iOS is typically configured on the
 * NSURLSession itself (via `URLSessionConfiguration`), or — for
 * stricter setups — handed off to NSURLSession's authentication-
 * challenge delegate. Wire that in the host app when needed; the
 * default here is pinning-disabled.
 */
val iosNetworkModule = module {

    single<PlatformHttpClientEngineFactory> {
        PlatformHttpClientEngineFactory { Darwin.create() }
    }
}

package io.github.halilozel1903.freetogame

import io.github.halilozel1903.freetogame.transport.FreeToGameTransport
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine

/** Settings for [FreeToGameClient], set in its builder block. */
public class FreeToGameConfig internal constructor() {
    /** The API root. */
    public var baseUrl: String = FreeToGameClient.DEFAULT_BASE_URL

    /**
     * The Ktor engine. `null` uses CIO. Pass `OkHttp.create()` (add `io.ktor:ktor-client-okhttp`) to share your
     * app's OkHttp stack, or a `MockEngine` in tests. The client does not close an engine you pass.
     */
    public var engine: HttpClientEngine? = null

    /** A ready Ktor client to use as is, instead of [engine]. [requestTimeoutMillis] and [retries] then do not apply. */
    public var httpClient: HttpClient? = null

    /** Replaces HTTP entirely: return the body of a url, for example from local files. */
    public var transport: FreeToGameTransport? = null

    /** Sent with every request; `null` keeps the engine's own User-Agent. */
    public var userAgent: String? = "freetogame-kotlin/1.0.0 (+https://github.com/halilozel1903/freetogame-kotlin)"

    /** Timeout of one request, in milliseconds. */
    public var requestTimeoutMillis: Long = 30_000

    /** Retries on network errors and 5xx responses, with exponential backoff. A challenge page is never retried. */
    public var retries: Int = 2

    /**
     * How long a response is kept in memory and reused, in milliseconds. FreeToGame's catalog changes a few times a
     * day, so the default is five minutes. `0` turns the cache off.
     */
    public var cacheTtlMillis: Long = 5 * 60_000

    /** FreeToGame asks for no more than 10 requests per second; requests are spaced to stay under this. `0` turns it off. */
    public var maxRequestsPerSecond: Int = 10
}

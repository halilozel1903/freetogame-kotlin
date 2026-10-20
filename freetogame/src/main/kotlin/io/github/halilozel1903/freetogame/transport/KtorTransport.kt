package io.github.halilozel1903.freetogame.transport

import io.github.halilozel1903.freetogame.FreeToGameException
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException

/** The default transport: GET requests with Ktor. Closes the client only when it created it. */
internal class KtorTransport(
    httpClient: HttpClient?,
    engine: HttpClientEngine?,
    private val userAgent: String?,
    requestTimeoutMillis: Long,
    retries: Int,
) : FreeToGameTransport, AutoCloseable {

    private val ownsClient = httpClient == null
    private val client: HttpClient = httpClient ?: run {
        val setup: io.ktor.client.HttpClientConfig<*>.() -> Unit = {
            expectSuccess = false
            install(HttpTimeout) {
                this.requestTimeoutMillis = requestTimeoutMillis
            }
            if (retries > 0) {
                install(HttpRequestRetry) {
                    // 5xx is retried, except a Cloudflare challenge (503 with cf-mitigated): asking again does not help.
                    retryIf(maxRetries = retries) { _, response ->
                        response.status.value in 500..599 && response.headers[CF_MITIGATED] == null
                    }
                    retryOnException(maxRetries = retries, retryOnTimeout = true)
                    exponentialDelay()
                }
            }
        }
        if (engine != null) HttpClient(engine, setup) else HttpClient(CIO, setup)
    }

    override suspend fun get(url: String): String {
        try {
            val response = client.get(url) {
                userAgent?.let { header(HttpHeaders.UserAgent, it) }
                accept(ContentType.Application.Json)
            }
            val body = response.bodyAsText()
            if (Cloudflare.isChallenge(body, response.headers[CF_MITIGATED])) {
                throw FreeToGameException.Blocked(url, response.status.value)
            }
            when {
                response.status == HttpStatusCode.NotFound -> throw FreeToGameException.NotFound(url)
                !response.status.isSuccess() -> throw FreeToGameException.Http(url, response.status.value)
            }
            return body
        } catch (e: FreeToGameException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw FreeToGameException.Network(url, e)
        }
    }

    override fun close() {
        if (ownsClient) client.close()
    }

    private companion object {
        const val CF_MITIGATED = "cf-mitigated"
    }
}

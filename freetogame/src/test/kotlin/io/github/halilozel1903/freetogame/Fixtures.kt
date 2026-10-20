package io.github.halilozel1903.freetogame

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Reads `src/test/resources/fixtures/<name>`. The fixtures are hand-written, abridged responses in the shape the
 * FreeToGame API sends; entries named "Example" are made up to cover edge cases. They are used by unit tests only.
 */
internal fun fixture(name: String): String =
    requireNotNull(Fixtures::class.java.getResource("/fixtures/$name")) { "No fixture $name" }.readText()

internal object Fixtures

/** Request urls (after the base url) and the fixture each one is answered with. */
internal val Routes = mapOf(
    "games?sort-by=popularity" to "games-popularity.json",
    "games?platform=windows&category=shooter&sort-by=popularity" to "games-popularity.json",
    "filter?tag=mmorpg.fantasy&platform=windows" to "filter-mmorpg-fantasy-windows.json",
    "filter?tag=mmorpg.fantasy.pvp.zombie&platform=browser&sort-by=release-date" to "no-results.json",
    "game?id=540" to "game-540.json",
    "game?id=345" to "game-345.json",
    "game?id=77" to "game-77.json",
)

/** A client on a Ktor [MockEngine] that serves [Routes] and answers 404 for anything else. */
internal class MockFreeToGame(
    cacheTtlMillis: Long = 60_000,
    private val override: (suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData?)? = null,
) : AutoCloseable {
    val requests: MutableList<HttpRequestData> = CopyOnWriteArrayList()

    val engine = MockEngine { request ->
        requests += request
        override?.invoke(this, request) ?: run {
            val name = Routes[request.url.toString().removePrefix(FreeToGameClient.DEFAULT_BASE_URL)]
            if (name == null) {
                respond(fixture("game-not-found.json"), HttpStatusCode.NotFound, headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                respond(fixture(name), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
    }

    val client = FreeToGameClient {
        engine = this@MockFreeToGame.engine
        this.cacheTtlMillis = cacheTtlMillis
        retries = 0
        maxRequestsPerSecond = 0
    }

    val urls: List<String> get() = requests.map { it.url.toString() }

    override fun close() {
        client.close()
        engine.close()
    }
}

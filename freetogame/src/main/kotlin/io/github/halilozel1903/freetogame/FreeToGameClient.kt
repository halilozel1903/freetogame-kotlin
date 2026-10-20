package io.github.halilozel1903.freetogame

import io.github.halilozel1903.freetogame.model.ApiStatus
import io.github.halilozel1903.freetogame.model.Game
import io.github.halilozel1903.freetogame.model.GameDetails
import io.github.halilozel1903.freetogame.transport.Cloudflare
import io.github.halilozel1903.freetogame.transport.FreeToGameTransport
import io.github.halilozel1903.freetogame.transport.KtorTransport
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.net.URLEncoder
import kotlin.coroutines.cancellation.CancellationException

/**
 * A coroutine client for the [FreeToGame API](https://www.freetogame.com/api-doc).
 *
 * ```kotlin
 * val client = FreeToGameClient()
 * val shooters = client.games(platform = Platform.Windows, category = GameTag.Shooter, sortBy = SortBy.Popularity)
 * val fantasyMmos = client.filter(GameTag.Mmorpg, GameTag.Fantasy, GameTag.Pvp, platform = Platform.Windows)
 * val details = client.game(shooters.first().id)
 * details.screenshots.forEach { println(it.image) }
 * println(details.minimumSystemRequirements?.graphics)
 * ```
 *
 * Every function is main-safe and throws a [FreeToGameException] on failure, including
 * [FreeToGameException.Blocked] when Cloudflare answers with a challenge page instead of data. Responses are kept
 * in memory for [FreeToGameConfig.cacheTtlMillis], and requests are spaced to stay under FreeToGame's limit of 10 per
 * second. Close the client when you no longer need it.
 *
 * FreeToGame's terms require attribution: show "Data provided by FreeToGame.com", linked to
 * https://www.freetogame.com, wherever you show its data.
 */
public class FreeToGameClient(configure: FreeToGameConfig.() -> Unit = {}) : AutoCloseable {

    private val config = FreeToGameConfig().apply(configure)

    /** The API root, always ending with a slash. */
    public val baseUrl: String = config.baseUrl.let { if (it.endsWith('/')) it else "$it/" }

    private val transport: FreeToGameTransport = config.transport ?: KtorTransport(
        httpClient = config.httpClient,
        engine = config.engine,
        userAgent = config.userAgent,
        requestTimeoutMillis = config.requestTimeoutMillis,
        retries = config.retries,
    )
    private val cacheLock = Mutex()
    private val cache = LinkedHashMap<String, Pair<Long, String>>()
    private val pacing = Mutex()
    private var lastRequestNanos = 0L

    // region Endpoints

    /**
     * `GET /games`: every live free-to-play game, optionally only those on [platform], in [category] (any
     * category or tag), sorted by [sortBy]. An empty list when nothing matches.
     */
    public suspend fun games(
        platform: Platform? = null,
        category: GameTag? = null,
        sortBy: SortBy? = null,
    ): List<Game> = fetchList(gamesUrl(platform, category, sortBy))

    /**
     * `GET /filter?tag=a.b.c`: games that have every one of [tags] (combined with `.`), optionally only on
     * [platform] and sorted by [sortBy]. An empty list when nothing matches.
     */
    public suspend fun filter(
        tags: Collection<GameTag>,
        platform: Platform? = null,
        sortBy: SortBy? = null,
    ): List<Game> = fetchList(filterUrl(tags, platform, sortBy))

    /** `GET /filter?tag=a.b.c` with the tags given one by one. */
    public suspend fun filter(
        vararg tags: GameTag,
        platform: Platform? = null,
        sortBy: SortBy? = null,
    ): List<Game> = filter(tags.toList(), platform, sortBy)

    /** `GET /game?id=`: one game with its description, screenshots and minimum system requirements. */
    public suspend fun game(id: Int): GameDetails {
        val url = gameUrl(id)
        val body = load(url)
        val element = parse(url, body)
        if (element is JsonObject && "id" !in element && ("status_message" in element || "status" in element)) {
            invalidate(url)
            throw FreeToGameException.NotFound(url)
        }
        return decode(url, GameDetails.serializer(), element)
    }

    /** The details of [game]: `GET /game?id={game.id}`. */
    public suspend fun details(game: Game): GameDetails = game(game.id)

    // endregion

    // region Urls

    /** The url [games] requests. Parameters that are `null` are left out. */
    public fun gamesUrl(platform: Platform? = null, category: GameTag? = null, sortBy: SortBy? = null): String =
        url(
            "games",
            "platform" to platform?.apiName,
            "category" to category?.apiName,
            "sort-by" to sortBy?.apiName,
        )

    /** The url [filter] requests. Repeated tags are sent once. */
    public fun filterUrl(tags: Collection<GameTag>, platform: Platform? = null, sortBy: SortBy? = null): String {
        val unique = tags.distinctBy { it.apiName.lowercase() }
        require(unique.isNotEmpty()) { "filter() needs at least one tag" }
        return url(
            "filter",
            "tag" to unique.joinToString(".") { it.apiName },
            "platform" to platform?.apiName,
            "sort-by" to sortBy?.apiName,
        )
    }

    /** The url [game] requests. */
    public fun gameUrl(id: Int): String {
        require(id > 0) { "FreeToGame ids start at 1, was $id" }
        return url("game", "id" to id.toString())
    }

    // endregion

    /** Removes every cached response. */
    public suspend fun clearCache() {
        cacheLock.withLock { cache.clear() }
    }

    /** Releases the HTTP client this instance created. */
    override fun close() {
        (transport as? AutoCloseable)?.close()
    }

    private fun url(path: String, vararg params: Pair<String, String?>): String {
        val query = params.mapNotNull { (key, value) -> value?.let { "$key=${encode(it)}" } }
        return if (query.isEmpty()) "$baseUrl$path" else "$baseUrl$path?${query.joinToString("&")}"
    }

    private suspend fun fetchList(url: String): List<Game> {
        val body = load(url)
        return when (val element = parse(url, body)) {
            is JsonArray -> decode(url, ListSerializer(Game.serializer()), element)
            // FreeToGame sends {"status":0,"status_message":"..."} when nothing matches.
            is JsonObject -> {
                val status = try {
                    json.decodeFromJsonElement(ApiStatus.serializer(), element)
                } catch (e: SerializationException) {
                    invalidate(url)
                    throw FreeToGameException.Decoding(url, e)
                }
                if (status.statusMessage == null && status.status == null) {
                    invalidate(url)
                    throw FreeToGameException.Decoding(url, IllegalStateException("Expected a list of games"))
                }
                emptyList()
            }
            else -> {
                invalidate(url)
                throw FreeToGameException.Decoding(url, IllegalStateException("Expected a list of games"))
            }
        }
    }

    private suspend fun parse(url: String, body: String): JsonElement {
        // A transport other than the default one may hand over a challenge page as a body.
        if (Cloudflare.isChallenge(body)) {
            invalidate(url)
            throw FreeToGameException.Blocked(url, 200)
        }
        return try {
            json.parseToJsonElement(body)
        } catch (e: SerializationException) {
            invalidate(url)
            throw FreeToGameException.Decoding(url, e)
        }
    }

    private suspend fun <T> decode(url: String, deserializer: DeserializationStrategy<T>, element: JsonElement): T = try {
        json.decodeFromJsonElement(deserializer, element)
    } catch (e: SerializationException) {
        invalidate(url)
        throw FreeToGameException.Decoding(url, e)
    } catch (e: IllegalArgumentException) {
        invalidate(url)
        throw FreeToGameException.Decoding(url, e)
    }

    private suspend fun load(url: String): String {
        val ttl = config.cacheTtlMillis
        if (ttl > 0) {
            cacheLock.withLock {
                val hit = cache[url]
                if (hit != null && System.currentTimeMillis() - hit.first < ttl) return hit.second
            }
        }
        val body = request(url)
        if (ttl > 0) {
            cacheLock.withLock {
                cache[url] = System.currentTimeMillis() to body
                while (cache.size > MAX_CACHE_ENTRIES) cache.remove(cache.keys.first())
            }
        }
        return body
    }

    private suspend fun invalidate(url: String) {
        cacheLock.withLock { cache.remove(url) }
    }

    private suspend fun request(url: String): String {
        awaitTurn()
        return try {
            transport.get(url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FreeToGameException) {
            throw e
        } catch (e: Exception) {
            throw FreeToGameException.Network(url, e)
        }
    }

    /** Spaces request starts by `1 / maxRequestsPerSecond` seconds. */
    private suspend fun awaitTurn() {
        val perSecond = config.maxRequestsPerSecond
        if (perSecond <= 0) return
        val spacing = 1_000_000_000L / perSecond
        pacing.withLock {
            val wait = lastRequestNanos + spacing - System.nanoTime()
            if (lastRequestNanos != 0L && wait > 0) delay((wait + 999_999) / 1_000_000)
            lastRequestNanos = System.nanoTime()
        }
    }

    public companion object {
        /** `https://www.freetogame.com/api/`. */
        public const val DEFAULT_BASE_URL: String = "https://www.freetogame.com/api/"

        /** The attribution FreeToGame's terms require next to its data. */
        public const val ATTRIBUTION: String = "Data provided by FreeToGame.com"

        /** The page [ATTRIBUTION] should link to. */
        public const val ATTRIBUTION_URL: String = "https://www.freetogame.com"

        private const val MAX_CACHE_ENTRIES = 64

        internal val json: Json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
            isLenient = true
        }

        private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8).replace("+", "%20")
    }
}

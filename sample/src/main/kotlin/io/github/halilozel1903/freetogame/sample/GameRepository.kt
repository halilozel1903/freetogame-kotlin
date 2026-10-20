package io.github.halilozel1903.freetogame.sample

import io.github.halilozel1903.freetogame.FreeToGameClient
import io.github.halilozel1903.freetogame.GameTag
import io.github.halilozel1903.freetogame.Platform
import io.github.halilozel1903.freetogame.SortBy
import io.github.halilozel1903.freetogame.model.Game
import io.github.halilozel1903.freetogame.model.GameDetails
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** A rail on the home screen. [genre] is `null` for the rails that are not a genre. */
data class GameRail(val key: String, val title: String, val hint: String, val genre: GameTag?, val games: List<Game>)

data class HomeData(val platform: Platform, val hero: GameDetails, val rails: List<GameRail>)

/** What the genre screen shows: a category, extra tags (then `/filter` is used), a platform and an order. */
data class GenreQuery(
    val genre: GameTag,
    val tags: List<GameTag> = emptyList(),
    val platform: Platform = Platform.All,
    val sortBy: SortBy = SortBy.Popularity,
)

data class GenrePage(val query: GenreQuery, val requestUrl: String, val games: List<Game>)

/** The genres of the home rails, in order. */
val HomeGenres = listOf(GameTag.Shooter, GameTag.Mmorpg, GameTag.Strategy, GameTag.Moba, GameTag.Racing, GameTag.Card)

/** Tags the genre screen offers to narrow a genre with `/filter`. */
val FilterTags = listOf(
    GameTag.Fantasy, GameTag.SciFi, GameTag.Pvp, GameTag.Pve, GameTag.OpenWorld, GameTag.ThreeD, GameTag.TwoD,
    GameTag.Anime, GameTag.Survival, GameTag.BattleRoyale, GameTag.LowSpec,
)

/** `null` for [Platform.All]: the API's default, so the parameter is left out. */
private val Platform.param: Platform? get() = takeIf { it != Platform.All }

/**
 * The sample's only data source: the live FreeToGame API through [FreeToGameClient]. Nothing is bundled and nothing
 * is made up; when the API cannot be reached (or Cloudflare answers with a challenge) the screen shows the error.
 */
class GameRepository(private val client: FreeToGameClient) {

    /** The hero (details of the most popular game) and the rails: popular, newest and one per [HomeGenres]. */
    suspend fun home(platform: Platform): HomeData = coroutineScope {
        val popular = async { client.games(platform.param, sortBy = SortBy.Popularity) }
        val newest = async { client.games(platform.param, sortBy = SortBy.ReleaseDate) }
        val genres = HomeGenres.map { genre -> async { genre to client.games(platform.param, genre, SortBy.Popularity) } }
        val top = popular.await()
        val first = top.firstOrNull() ?: error("FreeToGame returned no games for ${platform.label}.")
        val hero = client.details(first)
        val rails = buildList<GameRail> {
            add(GameRail("popular", "Popular now", "Most played on FreeToGame", null, top.drop(1).take(RailSize)))
            add(GameRail("newest", "New releases", "Latest first", null, newest.await().take(RailSize)))
            for ((genre, games) in genres.awaitAll()) {
                if (games.isNotEmpty()) add(GameRail("genre-${genre.apiName}", genre.label, "${games.size} games", genre, games.take(RailSize)))
            }
        }
        HomeData(platform, hero, rails)
    }

    /** A genre with optional tags: `/games?category=` without tags, `/filter?tag=genre.tag1.tag2` with them. */
    suspend fun genre(query: GenreQuery): GenrePage {
        return if (query.tags.isEmpty()) {
            val url = client.gamesUrl(query.platform.param, query.genre, query.sortBy)
            GenrePage(query, url, client.games(query.platform.param, query.genre, query.sortBy))
        } else {
            val tags = listOf(query.genre) + query.tags
            val url = client.filterUrl(tags, query.platform.param, query.sortBy)
            GenrePage(query, url, client.filter(tags, query.platform.param, query.sortBy))
        }
    }

    suspend fun details(id: Int): GameDetails = client.game(id)

    /**
     * For the detail scene: the most popular Windows game that has at least three screenshots and four requirement
     * fields, checked in order of popularity. Fails when none of the first ten has them.
     */
    suspend fun sceneDetails(): GameDetails {
        val candidates = client.games(Platform.Windows, sortBy = SortBy.Popularity).take(10)
        for (game in candidates) {
            val details = client.details(game)
            if (details.screenshots.size >= 3 && (details.requirementsOrNull?.entries?.size ?: 0) >= 4) return details
        }
        error("None of the ten most popular Windows games has screenshots and system requirements.")
    }

    companion object {
        const val RailSize = 20
    }
}

/** The app's repository, created on first use. */
object Repositories {
    private var repository: GameRepository? = null

    @Synchronized
    fun get(scene: Scene?): GameRepository = repository ?: GameRepository(
        FreeToGameClient {
            // Shares OkHttp with Coil.
            engine = OkHttp.create()
            userAgent = "freetogame-kotlin-sample/1.0.0 (+https://github.com/halilozel1903/freetogame-kotlin)"
            // More retries for a busy CI network in screenshot scenes. A Cloudflare challenge is never retried.
            retries = if (scene != null) 4 else 2
        },
    ).also { repository = it }
}

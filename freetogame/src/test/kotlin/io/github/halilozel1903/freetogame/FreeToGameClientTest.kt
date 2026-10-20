package io.github.halilozel1903.freetogame

import io.github.halilozel1903.freetogame.transport.FreeToGameTransport
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import java.io.IOException
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FreeToGameClientTest {

    @Test
    fun decodesTheGameList() = runTest {
        MockFreeToGame().use { api ->
            val games = api.client.games(sortBy = SortBy.Popularity)
            assertEquals(6, games.size)
            val overwatch = games.first()
            assertEquals(540, overwatch.id)
            assertEquals("Overwatch 2", overwatch.title)
            assertEquals("https://www.freetogame.com/g/540/thumbnail.jpg", overwatch.thumbnail)
            assertEquals("Shooter", overwatch.genre)
            assertEquals("Blizzard Entertainment", overwatch.developer)
            assertEquals(LocalDate.of(2022, 10, 4), overwatch.releaseDate)
            assertEquals(2022, overwatch.releaseYear)
            assertEquals("https://www.freetogame.com/overwatch-2", overwatch.profileUrl)
            assertEquals(setOf(Platform.Windows), overwatch.platforms)
            assertEquals(setOf(Platform.Browser), games.first { it.id == 345 }.platforms)
            val both = games.last()
            assertEquals(setOf(Platform.Windows, Platform.Browser), both.platforms)
            assertNull(both.releaseDate)
            assertEquals("https://www.freetogame.com/api/games?sort-by=popularity", api.urls.single())
        }
    }

    @Test
    fun buildsGamesQueries() = runTest {
        MockFreeToGame().use { api ->
            api.client.games(platform = Platform.Windows, category = GameTag.Shooter, sortBy = SortBy.Popularity)
            assertEquals("https://www.freetogame.com/api/games?platform=windows&category=shooter&sort-by=popularity", api.urls.single())
            assertTrue(api.requests.single().headers[HttpHeaders.UserAgent].orEmpty().startsWith("freetogame-kotlin/"))
        }
        val client = FreeToGameClient()
        client.use {
            assertEquals("https://www.freetogame.com/api/games", it.gamesUrl())
            assertEquals("https://www.freetogame.com/api/games?platform=browser", it.gamesUrl(platform = Platform.Browser))
            assertEquals("https://www.freetogame.com/api/games?category=open-world&sort-by=release-date", it.gamesUrl(category = GameTag.OpenWorld, sortBy = SortBy.ReleaseDate))
            assertEquals("https://www.freetogame.com/api/game?id=452", it.gameUrl(452))
        }
    }

    @Test
    fun filterCombinesTagsWithDots() = runTest {
        MockFreeToGame().use { api ->
            val games = api.client.filter(GameTag.Mmorpg, GameTag.Fantasy, platform = Platform.Windows)
            assertEquals(listOf(11, 21), games.map { it.id })
            assertEquals("https://www.freetogame.com/api/filter?tag=mmorpg.fantasy&platform=windows", api.urls.single())
        }
        FreeToGameClient().use { client ->
            assertEquals(
                "https://www.freetogame.com/api/filter?tag=3d.mmorpg.fantasy.pvp&platform=windows",
                client.filterUrl(listOf(GameTag.ThreeD, GameTag.Mmorpg, GameTag.Fantasy, GameTag.Pvp), Platform.Windows),
            )
            // Repeated tags are sent once.
            assertEquals(
                "https://www.freetogame.com/api/filter?tag=shooter.pvp&sort-by=alphabetical",
                client.filterUrl(listOf(GameTag.Shooter, GameTag.Pvp, GameTag("shooter")), sortBy = SortBy.Alphabetical),
            )
        }
    }

    @Test
    fun noMatchesIsAnEmptyList() = runTest {
        MockFreeToGame().use { api ->
            val games = api.client.filter(
                listOf(GameTag.Mmorpg, GameTag.Fantasy, GameTag.Pvp, GameTag.Zombie),
                platform = Platform.Browser,
                sortBy = SortBy.ReleaseDate,
            )
            assertTrue(games.isEmpty())
        }
    }

    @Test
    fun decodesGameDetails() = runTest {
        MockFreeToGame().use { api ->
            val game = api.client.game(540)
            assertEquals("Overwatch 2", game.title)
            assertEquals("Live", game.status)
            assertTrue(game.isLive)
            assertEquals(3, game.paragraphs.size)
            assertEquals("Teams of five fight over objectives across maps around the world.", game.paragraphs.last())
            assertEquals(listOf(1365, 1366, 1367), game.screenshots.map { it.id })
            assertEquals("https://www.freetogame.com/g/540/overwatch-2-1.png", game.screenshots.first().image)
            val requirements = game.minimumSystemRequirements!!
            assertEquals("Windows 10 64-bit", requirements.os)
            assertEquals("6 GB RAM", requirements.memory)
            assertEquals("50 GB available space", requirements.storage)
            assertEquals(listOf("OS", "Processor", "Memory", "Graphics", "Storage"), requirements.entries.map { it.first })
            assertEquals(setOf(Platform.Windows), game.platforms)
            assertEquals(540, game.toGame().id)
            assertEquals("https://www.freetogame.com/api/game?id=540", api.urls.single())
        }
    }

    @Test
    fun browserGamesHaveNoRequirements() = runTest {
        MockFreeToGame().use { api ->
            val game = api.client.game(345)
            assertNull(game.minimumSystemRequirements)
            assertNull(game.requirementsOrNull)
            assertEquals(1, game.screenshots.size)
            assertEquals(setOf(Platform.Browser), game.platforms)
        }
    }

    @Test
    fun toleratesMissingAndNullFields() = runTest {
        MockFreeToGame().use { api ->
            val game = api.client.game(77)
            assertEquals("", game.thumbnail)
            assertEquals("Beta", game.status)
            assertEquals(false, game.isLive)
            assertTrue(game.paragraphs.isEmpty())
            assertTrue(game.screenshots.isEmpty())
            // Null, empty and "?" values are left out.
            assertEquals(listOf("OS" to "Windows 7", "Storage" to "5 GB"), game.requirementsOrNull!!.entries)
        }
    }

    @Test
    fun cachesResponses() = runTest {
        MockFreeToGame().use { api ->
            api.client.game(540)
            api.client.game(540)
            assertEquals(1, api.requests.size)
            api.client.clearCache()
            api.client.game(540)
            assertEquals(2, api.requests.size)
        }
        MockFreeToGame(cacheTtlMillis = 0).use { api ->
            api.client.game(540)
            api.client.game(540)
            assertEquals(2, api.requests.size)
        }
    }

    @Test
    fun mapsErrors() = runTest {
        MockFreeToGame().use { api ->
            val notFound = assertFailsWith<FreeToGameException.NotFound> { api.client.game(123456) }
            assertEquals("https://www.freetogame.com/api/game?id=123456", notFound.url)
        }
        // A 200 with a status object instead of a game also means there is no such game.
        MockFreeToGame(override = { respond(fixture("game-not-found.json"), HttpStatusCode.OK) }).use { api ->
            assertFailsWith<FreeToGameException.NotFound> { api.client.game(1) }
        }
        MockFreeToGame(override = { respondError(HttpStatusCode.InternalServerError) }).use { api ->
            assertEquals(500, assertFailsWith<FreeToGameException.Http> { api.client.games() }.status)
        }
        MockFreeToGame(override = { respond("[{\"title\":\"no id\"}]", HttpStatusCode.OK) }).use { api ->
            assertFailsWith<FreeToGameException.Decoding> { api.client.games() }
            // A response that could not be decoded is not kept.
            assertFailsWith<FreeToGameException.Decoding> { api.client.games() }
            assertEquals(2, api.requests.size)
        }
        MockFreeToGame(override = { throw IOException("Connection reset") }).use { api ->
            val error = assertFailsWith<FreeToGameException.Network> { api.client.games() }
            assertTrue(error.cause is IOException)
        }
    }

    @Test
    fun cloudflareChallengeIsNeverData() = runTest {
        // A managed challenge: 403 with cf-mitigated and an HTML page.
        MockFreeToGame(override = {
            respond(
                fixture("cloudflare-challenge.html"),
                HttpStatusCode.Forbidden,
                headersOf(HttpHeaders.ContentType to listOf("text/html; charset=UTF-8"), "cf-mitigated" to listOf("challenge")),
            )
        }).use { api ->
            val error = assertFailsWith<FreeToGameException.Blocked> { api.client.games() }
            assertEquals(403, error.status)
            assertFailsWith<FreeToGameException.Blocked> { api.client.game(540) }
        }
        // The same page with a 200 and no header is still recognised.
        MockFreeToGame(override = { respond(fixture("cloudflare-challenge.html"), HttpStatusCode.OK) }).use { api ->
            assertFailsWith<FreeToGameException.Blocked> { api.client.filter(GameTag.Mmorpg) }
            assertFailsWith<FreeToGameException.Blocked> { api.client.filter(GameTag.Mmorpg) }
            assertEquals(2, api.requests.size)
        }
        // A custom transport that hands over the page as a body.
        FreeToGameClient { transport = FreeToGameTransport { fixture("cloudflare-challenge.html") } }.use { client ->
            assertFailsWith<FreeToGameException.Blocked> { client.game(540) }
        }
    }

    @Test
    fun rejectsInvalidArguments() = runTest {
        MockFreeToGame().use { api ->
            assertFailsWith<IllegalArgumentException> { api.client.game(0) }
            assertFailsWith<IllegalArgumentException> { api.client.filter(emptyList()) }
            assertTrue(api.requests.isEmpty())
        }
        assertFailsWith<IllegalArgumentException> { GameTag("mmorpg.fantasy") }
        assertFailsWith<IllegalArgumentException> { GameTag(" ") }
    }

    @Test
    fun customBaseUrl() = runTest {
        MockFreeToGame().use { api ->
            FreeToGameClient {
                baseUrl = "https://example.com/api"
                engine = api.engine
                retries = 0
            }.use { client ->
                runCatching { client.game(1) }
            }
            assertEquals("https://example.com/api/game?id=1", api.urls.single())
        }
    }
}

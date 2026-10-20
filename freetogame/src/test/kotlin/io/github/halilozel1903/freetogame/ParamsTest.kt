package io.github.halilozel1903.freetogame

import io.github.halilozel1903.freetogame.transport.Cloudflare
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ParamsTest {

    @Test
    fun tagLabels() {
        assertEquals("MMORPG", GameTag.Mmorpg.label)
        assertEquals("Open World", GameTag.OpenWorld.label)
        assertEquals("Sci-Fi", GameTag.SciFi.label)
        assertEquals("Third-Person", GameTag.ThirdPerson.label)
        assertEquals("Battle Royale", GameTag.BattleRoyale.label)
        assertEquals("3D", GameTag.ThreeD.label)
        assertEquals("Card Game", GameTag.Card.label)
        assertEquals(45, GameTag.all.size)
        assertEquals(GameTag.all.size, GameTag.all.map { it.apiName.lowercase() }.distinct().size)
    }

    @Test
    fun platformAndSortNames() {
        assertEquals(Platform.Windows, Platform.fromApiName("PC"))
        assertEquals(Platform.Windows, Platform.fromApiName("windows"))
        assertEquals(Platform.Browser, Platform.fromApiName(" Browser "))
        assertNull(Platform.fromApiName("mac"))
        assertEquals(SortBy.ReleaseDate, SortBy.fromApiName("release-date"))
        assertEquals(listOf("release-date", "popularity", "alphabetical", "relevance"), SortBy.entries.map { it.apiName })
    }

    @Test
    fun recognisesChallengePages() {
        assertTrue(Cloudflare.isChallenge(fixture("cloudflare-challenge.html")))
        assertTrue(Cloudflare.isChallenge("", mitigatedHeader = "challenge"))
        assertTrue(Cloudflare.isChallenge("<html><title>Attention Required! | Cloudflare</title></html>"))
        assertFalse(Cloudflare.isChallenge(fixture("games-popularity.json")))
        // A game description that mentions a challenge is not a challenge page.
        assertFalse(Cloudflare.isChallenge("[{\"short_description\":\"Just a moment... challenge-platform\"}]"))
        assertFalse(Cloudflare.isChallenge("<html><body>Other HTML</body></html>"))
    }
}

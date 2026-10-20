package io.github.halilozel1903.freetogame.transport

/**
 * Fetches the body of a FreeToGame url. The client uses a Ktor based transport by default; supply your own through
 * `FreeToGameConfig.transport` to serve responses from files, a database or another HTTP stack.
 *
 * Throw [io.github.halilozel1903.freetogame.FreeToGameException.NotFound] for missing games; other exceptions are
 * reported as [io.github.halilozel1903.freetogame.FreeToGameException.Network]. A body that is a Cloudflare
 * challenge page is still recognised and reported as
 * [io.github.halilozel1903.freetogame.FreeToGameException.Blocked].
 */
public fun interface FreeToGameTransport {
    public suspend fun get(url: String): String
}

package io.github.halilozel1903.freetogame

/** Every failure of [FreeToGameClient]. Catch the subclasses you care about, or all of them. */
public sealed class FreeToGameException(
    /** The requested url. */
    public val url: String,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /** FreeToGame answered 404, or said that there is no game with that id. */
    public class NotFound(url: String) : FreeToGameException(url, "Not found: $url")

    /** FreeToGame answered with another unsuccessful [status] (429, 5xx, ...). */
    public class Http(url: String, public val status: Int) : FreeToGameException(url, "HTTP $status for $url")

    /**
     * The answer was a Cloudflare challenge or block page instead of JSON. FreeToGame sits behind Cloudflare, which
     * sometimes challenges data center networks, VPNs or unusual clients. The client never treats such a page as
     * data. [status] is the HTTP status of the page.
     */
    public class Blocked(url: String, public val status: Int) :
        FreeToGameException(url, "FreeToGame answered with a Cloudflare challenge page (HTTP $status) for $url")

    /** The request failed before a response arrived (offline, timeout, DNS). */
    public class Network(url: String, cause: Throwable) :
        FreeToGameException(url, "Request failed for $url: ${cause.message ?: cause::class.simpleName}", cause)

    /** The response was not the JSON this library expects. */
    public class Decoding(url: String, cause: Throwable) :
        FreeToGameException(url, "Could not decode the response of $url: ${cause.message}", cause)
}

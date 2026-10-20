package io.github.halilozel1903.freetogame.transport

/** Recognises Cloudflare challenge and block pages, so they are never decoded or shown as data. */
public object Cloudflare {
    private val markers = listOf(
        "challenge-platform",
        "cf-chl-",
        "cf_chl_",
        "just a moment...",
        "attention required! | cloudflare",
        "cf-error-details",
        "enable javascript and cookies to continue",
    )

    /**
     * `true` when a response is a Cloudflare challenge or block page: the `cf-mitigated: challenge` header Cloudflare
     * sets on challenges, or an HTML body with Cloudflare's challenge markers.
     */
    public fun isChallenge(body: String, mitigatedHeader: String? = null): Boolean {
        if (mitigatedHeader?.trim().equals("challenge", ignoreCase = true)) return true
        val start = body.trimStart()
        if (!start.startsWith("<")) return false
        val lower = start.take(64_000).lowercase()
        return markers.any { it in lower }
    }
}

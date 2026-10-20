package io.github.halilozel1903.freetogame

/** The `platform` parameter of `/games` and `/filter`. */
public enum class Platform(
    /** The value sent to the API. */
    public val apiName: String,
    /** A short label for display. */
    public val label: String,
) {
    /** Games for Windows PCs. */
    Windows("windows", "Windows"),

    /** Games that run in a web browser. */
    Browser("browser", "Browser"),

    /** Both. The same as leaving the parameter out. */
    All("all", "All platforms"),
    ;

    public companion object {
        /** The platform with this [apiName], ignoring case (`"pc"` is read as [Windows]), or `null`. */
        public fun fromApiName(name: String): Platform? {
            val key = name.trim().lowercase()
            if (key == "pc") return Windows
            return entries.firstOrNull { it.apiName == key }
        }
    }
}

/** The `sort-by` parameter of `/games` and `/filter`. */
public enum class SortBy(
    /** The value sent to the API. */
    public val apiName: String,
    /** A short label for display. */
    public val label: String,
) {
    ReleaseDate("release-date", "Newest"),
    Popularity("popularity", "Most popular"),
    Alphabetical("alphabetical", "A to Z"),
    Relevance("relevance", "Relevance"),
    ;

    public companion object {
        /** The sort order with this [apiName], ignoring case, or `null`. */
        public fun fromApiName(name: String): SortBy? = entries.firstOrNull { it.apiName.equals(name.trim(), ignoreCase = true) }
    }
}

/**
 * A FreeToGame category or tag, such as `shooter`, `mmorpg` or `open-world`. The same values work as the `category`
 * of [FreeToGameClient.games] and as the tags of [FreeToGameClient.filter]. The constants are the values the API
 * documents; [GameTag] accepts any other value too, in case FreeToGame adds one.
 */
public data class GameTag(
    /** The value sent to the API. */
    public val apiName: String,
) {
    init {
        require(apiName.isNotBlank()) { "A tag must not be blank" }
        require('.' !in apiName && apiName.none { it.isWhitespace() }) {
            "A tag must not contain dots or spaces, was \"$apiName\". Pass several tags to filter() instead."
        }
    }

    /** A display label: `"open-world"` becomes `"Open World"`, `"mmorpg"` becomes `"MMORPG"`. */
    public val label: String
        get() = Labels[apiName.lowercase()] ?: apiName.split('-').joinToString(" ") { part ->
            part.lowercase().replaceFirstChar { it.uppercase() }
        }

    override fun toString(): String = apiName

    public companion object {
        public val Mmorpg: GameTag = GameTag("mmorpg")
        public val Shooter: GameTag = GameTag("shooter")
        public val Strategy: GameTag = GameTag("strategy")
        public val Moba: GameTag = GameTag("moba")
        public val Racing: GameTag = GameTag("racing")
        public val Sports: GameTag = GameTag("sports")
        public val Social: GameTag = GameTag("social")
        public val Sandbox: GameTag = GameTag("sandbox")
        public val OpenWorld: GameTag = GameTag("open-world")
        public val Survival: GameTag = GameTag("survival")
        public val Pvp: GameTag = GameTag("pvp")
        public val Pve: GameTag = GameTag("pve")
        public val Pixel: GameTag = GameTag("pixel")
        public val Voxel: GameTag = GameTag("voxel")
        public val Zombie: GameTag = GameTag("zombie")
        public val TurnBased: GameTag = GameTag("turn-based")
        public val FirstPerson: GameTag = GameTag("first-person")

        /** Written `third-Person` in the API documentation. */
        public val ThirdPerson: GameTag = GameTag("third-Person")
        public val TopDown: GameTag = GameTag("top-down")
        public val Tank: GameTag = GameTag("tank")
        public val Space: GameTag = GameTag("space")
        public val Sailing: GameTag = GameTag("sailing")
        public val SideScroller: GameTag = GameTag("side-scroller")
        public val Superhero: GameTag = GameTag("superhero")
        public val Permadeath: GameTag = GameTag("permadeath")
        public val Card: GameTag = GameTag("card")
        public val BattleRoyale: GameTag = GameTag("battle-royale")
        public val Mmo: GameTag = GameTag("mmo")
        public val Mmofps: GameTag = GameTag("mmofps")
        public val Mmotps: GameTag = GameTag("mmotps")
        public val ThreeD: GameTag = GameTag("3d")
        public val TwoD: GameTag = GameTag("2d")
        public val Anime: GameTag = GameTag("anime")
        public val Fantasy: GameTag = GameTag("fantasy")
        public val SciFi: GameTag = GameTag("sci-fi")
        public val Fighting: GameTag = GameTag("fighting")
        public val ActionRpg: GameTag = GameTag("action-rpg")
        public val Action: GameTag = GameTag("action")
        public val Military: GameTag = GameTag("military")
        public val MartialArts: GameTag = GameTag("martial-arts")
        public val Flight: GameTag = GameTag("flight")
        public val LowSpec: GameTag = GameTag("low-spec")
        public val TowerDefense: GameTag = GameTag("tower-defense")
        public val Horror: GameTag = GameTag("horror")
        public val Mmorts: GameTag = GameTag("mmorts")

        /** Every category and tag in the API documentation, in its order. */
        public val all: List<GameTag> = listOf(
            Mmorpg, Shooter, Strategy, Moba, Racing, Sports, Social, Sandbox, OpenWorld, Survival, Pvp, Pve, Pixel,
            Voxel, Zombie, TurnBased, FirstPerson, ThirdPerson, TopDown, Tank, Space, Sailing, SideScroller, Superhero,
            Permadeath, Card, BattleRoyale, Mmo, Mmofps, Mmotps, ThreeD, TwoD, Anime, Fantasy, SciFi, Fighting,
            ActionRpg, Action, Military, MartialArts, Flight, LowSpec, TowerDefense, Horror, Mmorts,
        )

        private val Labels = mapOf(
            "mmorpg" to "MMORPG",
            "moba" to "MOBA",
            "pvp" to "PvP",
            "pve" to "PvE",
            "mmo" to "MMO",
            "mmofps" to "MMOFPS",
            "mmotps" to "MMOTPS",
            "mmorts" to "MMORTS",
            "3d" to "3D",
            "2d" to "2D",
            "sci-fi" to "Sci-Fi",
            "action-rpg" to "Action RPG",
            "turn-based" to "Turn-Based",
            "first-person" to "First-Person",
            "third-person" to "Third-Person",
            "top-down" to "Top-Down",
            "side-scroller" to "Side-Scroller",
            "low-spec" to "Low-Spec",
            "card" to "Card Game",
        )
    }
}

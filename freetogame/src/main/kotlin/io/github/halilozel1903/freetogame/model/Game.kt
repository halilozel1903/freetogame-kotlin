package io.github.halilozel1903.freetogame.model

import io.github.halilozel1903.freetogame.Platform
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** Fields that every game has, in a list and in its details. */
public sealed interface GameInfo {
    public val id: Int
    public val title: String

    /** A 365 × 206 cover image. */
    public val thumbnail: String
    public val shortDescription: String

    /** Where the game can be played or downloaded. */
    public val gameUrl: String

    /** The main genre as FreeToGame shows it, such as `"Shooter"`, `"MMORPG"` or `"Card Game"`. */
    public val genre: String

    /** `"PC (Windows)"`, `"Web Browser"`, both joined with a comma, or `"Windows"` in details. */
    public val platform: String
    public val publisher: String
    public val developer: String

    /** `yyyy-MM-dd` as sent by the API; see [releaseDate]. */
    public val releaseDateText: String

    /** The game's page on FreeToGame. Link it when you show the game: FreeToGame asks for attribution. */
    public val profileUrl: String

    /** The platforms named in [platform]. */
    public val platforms: Set<Platform>
        get() = buildSet<Platform> {
            val text = platform.lowercase()
            if ("windows" in text || "pc" in text.split(' ', '(', ')', ',')) add(Platform.Windows)
            if ("browser" in text || "web" in text) add(Platform.Browser)
        }

    /** [releaseDateText] as a date, or `null` when it is missing or not a valid date. */
    public val releaseDate: LocalDate?
        get() = try {
            LocalDate.parse(releaseDateText.trim())
        } catch (e: DateTimeParseException) {
            null
        }

    /** The release year, or `null`. */
    public val releaseYear: Int? get() = releaseDate?.year
}

/** One entry of `/games` and `/filter`. */
@Serializable
public data class Game(
    override val id: Int,
    override val title: String,
    override val thumbnail: String = "",
    @SerialName("short_description") override val shortDescription: String = "",
    @SerialName("game_url") override val gameUrl: String = "",
    override val genre: String = "",
    override val platform: String = "",
    override val publisher: String = "",
    override val developer: String = "",
    @SerialName("release_date") override val releaseDateText: String = "",
    @SerialName("freetogame_profile_url") override val profileUrl: String = "",
) : GameInfo

/** `/game?id=`: a game with its full description, screenshots and minimum system requirements. */
@Serializable
public data class GameDetails(
    override val id: Int,
    override val title: String,
    override val thumbnail: String = "",
    /** `"Live"`, or another status FreeToGame gives games that are in beta or shutting down. */
    val status: String? = null,
    @SerialName("short_description") override val shortDescription: String = "",
    /** The long description, with paragraphs separated by line breaks. See [paragraphs]. */
    val description: String = "",
    @SerialName("game_url") override val gameUrl: String = "",
    override val genre: String = "",
    override val platform: String = "",
    override val publisher: String = "",
    override val developer: String = "",
    @SerialName("release_date") override val releaseDateText: String = "",
    @SerialName("freetogame_profile_url") override val profileUrl: String = "",
    /** `null` for most browser games, which have no requirements listed. */
    @SerialName("minimum_system_requirements") val minimumSystemRequirements: SystemRequirements? = null,
    val screenshots: List<Screenshot> = emptyList(),
) : GameInfo {

    /** [description] split into paragraphs, with blank lines and surrounding spaces removed. */
    public val paragraphs: List<String>
        get() = description.replace("\r\n", "\n").replace('\r', '\n')
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    /** `true` when [status] is `"Live"` or missing. */
    public val isLive: Boolean get() = status == null || status.equals("live", ignoreCase = true)

    /** [minimumSystemRequirements] when at least one of its fields has a value, otherwise `null`. */
    public val requirementsOrNull: SystemRequirements?
        get() = minimumSystemRequirements?.takeIf { it.entries.isNotEmpty() }

    /** The same game as a list entry. */
    public fun toGame(): Game = Game(
        id = id,
        title = title,
        thumbnail = thumbnail,
        shortDescription = shortDescription,
        gameUrl = gameUrl,
        genre = genre,
        platform = platform,
        publisher = publisher,
        developer = developer,
        releaseDateText = releaseDateText,
        profileUrl = profileUrl,
    )
}

/** One screenshot of [GameDetails.screenshots]. */
@Serializable
public data class Screenshot(
    val id: Int,
    /** The image url, usually a JPEG of 1280 pixels or more in width. */
    val image: String,
)

/** The minimum system requirements of a Windows game. Any field can be missing or `null`. */
@Serializable
public data class SystemRequirements(
    val os: String? = null,
    val processor: String? = null,
    val memory: String? = null,
    val graphics: String? = null,
    val storage: String? = null,
) {
    /** The requirements that have a value, labelled for display, in the order the API sends them. */
    public val entries: List<Pair<String, String>>
        get() = listOf("OS" to os, "Processor" to processor, "Memory" to memory, "Graphics" to graphics, "Storage" to storage)
            .mapNotNull { (label, value) -> value?.trim()?.takeIf { it.isNotEmpty() && it != "?" }?.let { label to it } }
}

/** The object FreeToGame sends instead of a list when nothing matches, or instead of a game that does not exist. */
@Serializable
internal data class ApiStatus(
    val status: Int? = null,
    @SerialName("status_message") val statusMessage: String? = null,
)

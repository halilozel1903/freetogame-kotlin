package io.github.halilozel1903.freetogame.sample

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import io.github.halilozel1903.freetogame.FreeToGameClient
import io.github.halilozel1903.freetogame.model.Game

/** Left and right margin of every screen. */
val ScreenPadding = 48.dp

private val CardShape = RoundedCornerShape(10.dp)

/** A rounded, focusable pill: buttons and filter chips. [selected] fills it like [primary]. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    selected: Boolean = false,
    compact: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val filled = primary || selected
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (filled) colors.primary else Color.White.copy(alpha = 0.10f),
            contentColor = if (filled) colors.onPrimary else colors.onSurface,
            focusedContainerColor = colors.onSurface,
            focusedContentColor = colors.background,
            pressedContainerColor = colors.onSurface.copy(alpha = 0.8f),
            pressedContentColor = colors.background,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, colors.onSurface), shape = CircleShape),
        ),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = if (compact) 14.dp else 20.dp, vertical = if (compact) 6.dp else 9.dp),
            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = LocalContentColor.current,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A small, not focusable label, such as a genre or a platform. */
@Composable
fun Tag(text: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (accent) colors.primary.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = if (accent) colors.primary else colors.onSurfaceVariant,
        maxLines = 1,
    )
}

/** "Data provided by FreeToGame.com": FreeToGame's terms require it wherever its data is shown. */
@Composable
fun Attribution(modifier: Modifier = Modifier) {
    Text(
        text = "${FreeToGameClient.ATTRIBUTION} · freetogame.com",
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
    )
}

/**
 * A focusable game card: the 16:9 thumbnail in a frame that scales up, glows and gets a border when focused, with
 * the title and "genre · platform" below it.
 */
@Composable
fun GameCard(game: Game, onClick: () -> Unit, modifier: Modifier = Modifier, width: Dp? = null) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme
    Column(if (width != null) modifier.width(width) else modifier) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(365f / 206f),
            shape = CardDefaults.shape(shape = CardShape),
            colors = CardDefaults.colors(containerColor = colors.surfaceVariant),
            scale = CardDefaults.scale(focusedScale = 1.08f),
            border = CardDefaults.border(
                focusedBorder = Border(border = BorderStroke(3.dp, colors.onSurface), shape = CardShape),
            ),
            glow = CardDefaults.glow(
                focusedGlow = Glow(elevationColor = colors.primary.copy(alpha = 0.45f), elevation = 14.dp),
            ),
            interactionSource = source,
        ) {
            GameImage(game.thumbnail, game.title, Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(if (focused) 10.dp else 6.dp))
        Text(
            text = game.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
            color = if (focused) colors.onSurface else colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = listOf(game.genre, platformLabel(game)).filter { it.isNotBlank() }.joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A card at the end of a genre rail that opens the whole genre. */
@Composable
fun SeeAllCard(label: String, onClick: () -> Unit, width: Dp) {
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(width)
            .aspectRatio(365f / 206f),
        shape = CardDefaults.shape(shape = CardShape),
        colors = CardDefaults.colors(containerColor = colors.surfaceVariant),
        scale = CardDefaults.scale(focusedScale = 1.08f),
        border = CardDefaults.border(
            focusedBorder = Border(border = BorderStroke(3.dp, colors.onSurface), shape = CardShape),
        ),
    ) {
        Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.primary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** "Windows", "Browser" or "Windows, Browser". */
fun platformLabel(game: io.github.halilozel1903.freetogame.model.GameInfo): String =
    game.platforms.sortedBy { it.ordinal }.joinToString(", ") { it.label }.ifEmpty { game.platform }

/** A titled, horizontally scrolling row of [GameCard]s, with a "See all" card when [onSeeAll] is set. */
@Composable
fun GameRailRow(
    title: String,
    hint: String?,
    games: List<Game>,
    onOpenGame: (Game) -> Unit,
    cardWidth: Dp,
    modifier: Modifier = Modifier,
    onSeeAll: (() -> Unit)? = null,
    seeAllLabel: String = "See all",
) {
    if (games.isEmpty()) return
    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(horizontal = ScreenPadding), verticalAlignment = Alignment.Bottom) {
            Text(
                text = title,
                modifier = Modifier.weight(1f, fill = false).semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (hint != null) {
                Spacer(Modifier.width(12.dp))
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = ScreenPadding, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(games, key = { it.id }) { game ->
                GameCard(game, onClick = { onOpenGame(game) }, width = cardWidth)
            }
            if (onSeeAll != null) {
                item(key = "see-all") { SeeAllCard(seeAllLabel, onSeeAll, cardWidth) }
            }
        }
    }
}

/** A small round dot between facts. */
@Composable
fun Dot() {
    Box(
        Modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)),
    )
}

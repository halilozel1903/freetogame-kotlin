package io.github.halilozel1903.freetogame.sample

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.halilozel1903.freetogame.model.GameDetails
import io.github.halilozel1903.freetogame.model.Screenshot
import io.github.halilozel1903.freetogame.model.SystemRequirements

/**
 * A game's details: the screenshot gallery (the focused thumbnail fills the large viewer), facts, the description and
 * the minimum system requirements. With `id == null` it shows the game the detail scene picks.
 */
@Composable
fun DetailScreen(id: Int?, repository: GameRepository, onBack: () -> Unit) {
    var attempt by remember { mutableIntStateOf(0) }
    val state by produceState<Load<GameDetails>>(Load.Loading, repository, id, attempt) {
        value = Load.Loading
        value = load { if (id == null) repository.sceneDetails() else repository.details(id) }
    }
    ReportLoad(state)
    when (val current = state) {
        Load.Loading -> LoadingState()
        is Load.Failed -> ErrorState(current.message, onRetry = { attempt++ }, onBack = onBack)
        is Load.Ready -> DetailContent(current.value)
    }
}

private val GalleryShape = RoundedCornerShape(10.dp)

@Composable
private fun DetailContent(game: GameDetails) {
    val colors = MaterialTheme.colorScheme
    val shots = game.screenshots.ifEmpty { listOf(Screenshot(0, game.thumbnail)) }
    var selected by rememberSaveable(game.id) { mutableIntStateOf(0) }
    val firstShot = remember { FocusRequester() }
    FocusOnce(firstShot, game.id)

    Row(
        Modifier
            .fillMaxSize()
            .padding(start = ScreenPadding, end = ScreenPadding, top = 24.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        // The gallery.
        Column(Modifier.weight(0.56f)) {
            GameImage(
                url = shots[selected.coerceIn(0, shots.lastIndex)].image,
                contentDescription = "${game.title} screenshot ${selected + 1}",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(GalleryShape),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Screenshots · ${selected + 1} of ${shots.size}",
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant,
            )
            LazyRow(
                // Room for the focused thumbnail's 1.1x scale and border, so neither is clipped at the row's edges.
                contentPadding = PaddingValues(vertical = 10.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(shots, key = { index, shot -> "${shot.id}-$index" }) { index, shot ->
                    Card(
                        onClick = { selected = index },
                        modifier = Modifier
                            .width(112.dp)
                            .aspectRatio(16f / 9f)
                            .then(if (index == 0) Modifier.focusRequester(firstShot) else Modifier)
                            .onFocusChanged { if (it.isFocused) selected = index },
                        shape = CardDefaults.shape(shape = RoundedCornerShape(6.dp)),
                        scale = CardDefaults.scale(focusedScale = 1.1f),
                        border = CardDefaults.border(
                            border = if (index == selected) Border(BorderStroke(2.dp, colors.primary), shape = RoundedCornerShape(6.dp)) else Border.None,
                            focusedBorder = Border(BorderStroke(3.dp, colors.onSurface), shape = RoundedCornerShape(6.dp)),
                        ),
                    ) {
                        GameImage(shot.image, null, Modifier.fillMaxSize())
                    }
                }
            }
        }
        // Facts and requirements.
        Column(Modifier.weight(0.44f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = game.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Tag(game.genre, accent = true)
                Tag(platformLabel(game))
                game.status?.let { Tag(it) }
                game.releaseDate?.let { Tag(it.toString()) }
            }
            Text(
                text = listOf(game.developer, game.publisher).filter { it.isNotBlank() }.distinct().joinToString(" · "),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = game.paragraphs.firstOrNull() ?: game.shortDescription,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Requirements(game.requirementsOrNull)
            Spacer(Modifier.weight(1f))
            Attribution()
        }
    }
}

@Composable
private fun Requirements(requirements: SystemRequirements?) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = "Minimum system requirements",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        val entries = requirements?.entries.orEmpty()
        if (entries.isEmpty()) {
            Text(
                text = "None listed. Browser games run in any modern browser.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        for ((label, value) in entries) {
            Row {
                Text(
                    text = label,
                    modifier = Modifier.width(84.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

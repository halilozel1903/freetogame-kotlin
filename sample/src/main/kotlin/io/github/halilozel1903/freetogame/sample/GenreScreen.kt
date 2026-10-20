package io.github.halilozel1903.freetogame.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.halilozel1903.freetogame.Platform
import io.github.halilozel1903.freetogame.SortBy

/**
 * One genre as a grid. The sort order and the platform change the `/games` query; picking tags switches to
 * `/filter?tag=genre.tag1.tag2`, which only lists games that have every tag. The request is shown under the title.
 */
@Composable
fun GenreScreen(
    query: GenreQuery,
    repository: GameRepository,
    onQueryChange: (GenreQuery) -> Unit,
    onOpenGame: (Int) -> Unit,
    onBack: () -> Unit,
) {
    var attempt by remember { mutableIntStateOf(0) }
    val state by produceState<Load<GenrePage>>(Load.Loading, repository, query, attempt) {
        value = Load.Loading
        value = load { repository.genre(query) }
    }
    ReportLoad(state)
    Column(Modifier.fillMaxSize()) {
        GenreHeader(query, (state as? Load.Ready)?.value, onQueryChange)
        when (val current = state) {
            Load.Loading -> LoadingState()
            is Load.Failed -> ErrorState(current.message, onRetry = { attempt++ }, onBack = onBack)
            is Load.Ready -> GenreGrid(current.value, onOpenGame)
        }
    }
}

@Composable
private fun GenreHeader(query: GenreQuery, page: GenrePage?, onQueryChange: (GenreQuery) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
        Row(Modifier.padding(horizontal = ScreenPadding), verticalAlignment = Alignment.Bottom) {
            Text(
                text = (listOf(query.genre) + query.tags).reversed().joinToString(" ") { it.label },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = page?.let { "${it.games.size} games match" } ?: "Loading…",
                style = MaterialTheme.typography.titleSmall,
                color = colors.primary,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Text(
            text = page?.requestUrl?.let { "GET " + it.substringAfter("/api") } ?: " ",
            modifier = Modifier.padding(horizontal = ScreenPadding),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = ScreenPadding, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(SortBy.entries, key = { "sort-${it.name}" }) { sort ->
                PillButton(sort.label, onClick = { onQueryChange(query.copy(sortBy = sort)) }, selected = sort == query.sortBy, compact = true)
            }
            item(key = "gap") { Spacer(Modifier.width(16.dp)) }
            items(listOf(Platform.All, Platform.Windows, Platform.Browser), key = { "platform-${it.name}" }) { platform ->
                PillButton(platform.label, onClick = { onQueryChange(query.copy(platform = platform)) }, selected = platform == query.platform, compact = true)
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = ScreenPadding, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item(key = "label") {
                Text("Tags", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            }
            items(FilterTags.filter { it != query.genre }, key = { "tag-${it.apiName}" }) { tag ->
                val selected = tag in query.tags
                PillButton(
                    text = (if (selected) "✓ " else "+ ") + tag.label,
                    onClick = { onQueryChange(query.copy(tags = if (selected) query.tags - tag else query.tags + tag)) },
                    selected = selected,
                    compact = true,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun GenreGrid(page: GenrePage, onOpenGame: (Int) -> Unit) {
    if (page.games.isEmpty()) {
        Text(
            text = "No game has all of these tags. Remove a tag to see more.",
            modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 32.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val firstCard = remember { FocusRequester() }
    FocusOnce(firstCard, page.query)
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        state = rememberLazyGridState(),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = 8.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        itemsIndexed(page.games, key = { _, game -> game.id }) { index, game ->
            GameCard(
                game = game,
                onClick = { onOpenGame(game.id) },
                modifier = if (index == 0) Modifier.focusRequester(firstCard) else Modifier,
            )
        }
    }
}

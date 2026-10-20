package io.github.halilozel1903.freetogame.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.halilozel1903.freetogame.GameTag
import io.github.halilozel1903.freetogame.Platform
import io.github.halilozel1903.freetogame.model.GameDetails

/** Rail cards are small enough that the hero and two rails fit on a 1080p screen. */
private val HomeCardWidth = 176.dp

/** The hero for the most popular game, a platform filter, and rails: popular, newest and one per genre. */
@Composable
fun HomeScreen(
    repository: GameRepository,
    platform: Platform,
    onPlatformChange: (Platform) -> Unit,
    onOpenGame: (Int) -> Unit,
    onOpenGenre: (GameTag) -> Unit,
) {
    var attempt by remember { mutableIntStateOf(0) }
    val state by produceState<Load<HomeData>>(Load.Loading, repository, platform, attempt) {
        value = Load.Loading
        value = load { repository.home(platform) }
    }
    ReportLoad(state)
    when (val current = state) {
        Load.Loading -> LoadingState()
        is Load.Failed -> ErrorState(current.message, onRetry = { attempt++ }, onBack = null)
        is Load.Ready -> HomeContent(current.value, onPlatformChange, onOpenGame, onOpenGenre)
    }
}

@Composable
private fun HomeContent(
    data: HomeData,
    onPlatformChange: (Platform) -> Unit,
    onOpenGame: (Int) -> Unit,
    onOpenGenre: (GameTag) -> Unit,
) {
    val heroFocus = remember { FocusRequester() }
    // Focus the hero once per platform; coming back from another screen keeps the focus where it was.
    var focusedPlatform by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(data.platform) {
        if (focusedPlatform != data.platform.name) {
            withFrameNanos { }
            withFrameNanos { }
            runCatching { heroFocus.requestFocus() }
            focusedPlatform = data.platform.name
        }
    }
    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "hero") {
            Hero(
                game = data.hero,
                platform = data.platform,
                onPlatformChange = onPlatformChange,
                onDetails = { onOpenGame(data.hero.id) },
                onGenre = HomeGenres.firstOrNull { it.label.equals(data.hero.genre, ignoreCase = true) }?.let { genre -> { onOpenGenre(genre) } },
                buttonModifier = Modifier.focusRequester(heroFocus),
            )
        }
        for (rail in data.rails) {
            item(key = rail.key) {
                GameRailRow(
                    title = rail.title,
                    hint = rail.hint,
                    games = rail.games,
                    onOpenGame = { onOpenGame(it.id) },
                    cardWidth = HomeCardWidth,
                    onSeeAll = rail.genre?.let { genre -> { onOpenGenre(genre) } },
                    seeAllLabel = "All ${rail.title} games",
                )
            }
        }
        item(key = "attribution") {
            Attribution(Modifier.padding(horizontal = ScreenPadding, vertical = 8.dp))
        }
    }
}

@Composable
private fun Hero(
    game: GameDetails,
    platform: Platform,
    onPlatformChange: (Platform) -> Unit,
    onDetails: () -> Unit,
    onGenre: (() -> Unit)?,
    buttonModifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .height(262.dp),
    ) {
        // The first screenshot is much sharper than the 365 px thumbnail on a TV.
        GameImage(
            url = game.screenshots.firstOrNull()?.image ?: game.thumbnail,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.64f),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        0f to colors.background,
                        0.36f to colors.background,
                        0.66f to colors.background.copy(alpha = 0.45f),
                        1f to Color.Transparent,
                    ),
                ),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to colors.background)),
        )
        Column(Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Free Games TV",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                    Attribution()
                }
                // The platform filter: every rail reloads with ?platform=.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (option in Platform.entries.sortedBy { if (it == Platform.All) -1 else it.ordinal }) {
                        PillButton(
                            text = option.label,
                            onClick = { onPlatformChange(option) },
                            selected = option == platform,
                            compact = true,
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Column(Modifier.width(470.dp)) {
                Text(
                    text = "#1 most popular" + if (platform == Platform.All) "" else " on ${platform.label}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primary,
                )
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Tag(game.genre, accent = true)
                    Tag(platformLabel(game))
                    game.releaseYear?.let { Tag(it.toString()) }
                    Text(
                        text = game.publisher,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = game.shortDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    PillButton(text = "Details", onClick = onDetails, modifier = buttonModifier, primary = true)
                    if (onGenre != null) PillButton(text = "More ${game.genre}", onClick = onGenre)
                }
            }
        }
    }
}

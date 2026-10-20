package io.github.halilozel1903.freetogame.sample

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import io.github.halilozel1903.freetogame.GameTag
import io.github.halilozel1903.freetogame.Platform

/** The app: a small back stack of [Screen]s. The platform picked on home carries over to genre pages. */
@Composable
fun SampleApp(scene: Scene?, repository: GameRepository) {
    val stack = remember {
        mutableStateListOf<Screen>().apply { addAll(scene?.backStack() ?: listOf(Screen.Home)) }
    }
    var platform by remember { mutableStateOf(Platform.All) }
    val stateHolder = rememberSaveableStateHolder()

    val open: (Screen) -> Unit = { stack.add(it) }
    val back: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    BackHandler(enabled = stack.size > 1) { back() }

    val screen = stack.last()
    stateHolder.SaveableStateProvider(screen.toString()) {
        when (screen) {
            Screen.Home -> HomeScreen(
                repository = repository,
                platform = platform,
                onPlatformChange = { platform = it },
                onOpenGame = { open(Screen.Detail(it)) },
                onOpenGenre = { genre: GameTag -> open(Screen.Genre(GenreQuery(genre, platform = platform))) },
            )
            is Screen.Genre -> GenreScreen(
                query = screen.query,
                repository = repository,
                // Changing the query replaces this screen instead of growing the back stack.
                onQueryChange = { stack[stack.lastIndex] = Screen.Genre(it) },
                onOpenGame = { open(Screen.Detail(it)) },
                onBack = back,
            )
            is Screen.Detail -> DetailScreen(id = screen.id, repository = repository, onBack = back)
        }
    }
}

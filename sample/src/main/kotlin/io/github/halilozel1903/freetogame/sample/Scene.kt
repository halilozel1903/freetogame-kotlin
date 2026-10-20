package io.github.halilozel1903.freetogame.sample

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import io.github.halilozel1903.freetogame.GameTag
import io.github.halilozel1903.freetogame.Platform
import io.github.halilozel1903.freetogame.SortBy
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * Screenshot scenes, picked with the `scene` intent extra (see `scripts/screenshots.sh`). They load the live
 * FreeToGame API and its images exactly like a normal launch; there is no bundled or fallback data.
 *
 * @property minImages how many images must have loaded on screen before the scene is complete.
 */
enum class Scene(val key: String, val minImages: Int) {
    /** Home for all platforms: the hero for the most popular game and the first rails. */
    Home("home", minImages = 6),

    /** Fantasy MMORPGs for Windows, most popular first: `GET /filter?tag=mmorpg.fantasy&platform=windows`. */
    Genre("genre", minImages = 6),

    /** The most popular Windows game that has screenshots and minimum requirements. */
    Detail("detail", minImages = 4),
    ;

    /** The screens to open, bottom of the back stack first. */
    fun backStack(): List<Screen> = when (this) {
        Home -> listOf(Screen.Home)
        Genre -> listOf(Screen.Home, Screen.Genre(GenreQuery(GameTag.Mmorpg, listOf(GameTag.Fantasy), Platform.Windows, SortBy.Popularity)))
        Detail -> listOf(Screen.Home, Screen.Detail(id = null))
    }

    companion object {
        fun from(key: String?): Scene? = entries.firstOrNull { it.key == key }
    }
}

/** One screen of the app's back stack. */
sealed interface Screen {
    data object Home : Screen

    data class Genre(val query: GenreQuery) : Screen

    /** A game's details; `null` picks the game for the detail scene. */
    data class Detail(val id: Int?) : Screen
}

/** Where an image of a screenshot scene is. */
enum class ImageStatus { Loading, Loaded, Failed }

/**
 * Tells `scripts/screenshots.sh` when a screenshot scene is complete: every load on screen came back from the live
 * API and every image on screen has loaded. The script waits for `SCENE_READY <scene>` in logcat before it captures
 * and fails on `SCENE_FAILED`, so an error screen, a Cloudflare challenge or a missing image never ends up in the
 * README.
 */
class SceneMonitor(private val scene: Scene) {
    private val loads = mutableStateMapOf<Any, Load<*>>()
    private val images = mutableStateMapOf<Any, ImageStatus>()

    fun load(id: Any, state: Load<*>?) {
        if (state == null) loads.remove(id) else loads[id] = state
    }

    fun image(id: Any, status: ImageStatus?) {
        if (status == null) images.remove(id) else images[id] = status
    }

    private sealed interface Status {
        data object Waiting : Status
        data object Ready : Status
        data class Failed(val reason: String) : Status
    }

    private fun status(): Status {
        val failed = loads.values.filterIsInstance<Load.Failed>().firstOrNull()
        return when {
            failed != null -> Status.Failed(failed.message)
            images.values.any { it == ImageStatus.Failed } -> Status.Failed("An image did not load.")
            loads.isNotEmpty() &&
                loads.values.all { it is Load.Ready } &&
                images.size >= scene.minImages &&
                images.values.all { it == ImageStatus.Loaded } -> Status.Ready
            else -> Status.Waiting
        }
    }

    @Composable
    fun Report() {
        LaunchedEffect(this) {
            snapshotFlow { status() }.collectLatest { status ->
                when (status) {
                    Status.Waiting -> Unit
                    // Ready for a moment without anything new loading: layout and focus have settled.
                    Status.Ready -> {
                        delay(2_000)
                        withFrameNanos { }
                        Log.i(TAG, "SCENE_READY ${scene.key}")
                    }
                    is Status.Failed -> Log.e(TAG, "SCENE_FAILED ${scene.key}: ${status.reason}")
                }
            }
        }
    }

    companion object {
        const val TAG = "FreeToGameScene"
    }
}

/** The monitor of the screenshot scene being shown, or `null` in normal use. */
val LocalSceneMonitor = staticCompositionLocalOf<SceneMonitor?> { null }

/** Reports [state] to the scene monitor, if there is one. */
@Composable
fun ReportLoad(state: Load<*>) {
    val monitor = LocalSceneMonitor.current ?: return
    val id = remember { Any() }
    DisposableEffect(monitor, id) { onDispose { monitor.load(id, null) } }
    LaunchedEffect(monitor, id, state) { monitor.load(id, state) }
}

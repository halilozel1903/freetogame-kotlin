package io.github.halilozel1903.freetogame.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.darkColorScheme

/**
 * An Android TV browser for free-to-play games, built on freetogame-kotlin. Everything comes from the live FreeToGame
 * API, with the thumbnails and screenshots loaded by Coil.
 *
 * D-pad presses can't be timed reliably through adb on a fresh emulator, so `scripts/screenshots.sh` starts the app
 * with `--es scene <scene>` to open a fixed screen, loaded live like a normal launch:
 *
 * - `home`: the hero for the most popular game, the platform filter and the first rails, "Details" focused
 * - `genre`: fantasy MMORPGs for Windows, most popular first (`/filter?tag=mmorpg.fantasy&platform=windows`)
 * - `detail`: the most popular Windows game with screenshots and requirements
 *
 * A scene logs `SCENE_READY` once its data and every image on screen have loaded, and `SCENE_FAILED` when a request
 * fails or FreeToGame answers with a Cloudflare challenge. There is no fallback data.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scene = Scene.from(intent.getStringExtra(EXTRA_SCENE))
        val repository = Repositories.get(scene)
        setContent {
            MaterialTheme(colorScheme = SampleColors) {
                // The Surface gives every Text a readable default color on the dark background.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape,
                    colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                ) {
                    val monitor = remember { scene?.let { SceneMonitor(it) } }
                    CompositionLocalProvider(LocalSceneMonitor provides monitor) {
                        SampleApp(scene, repository)
                    }
                    monitor?.Report()
                }
            }
        }
    }

    companion object {
        const val EXTRA_SCENE = "scene"
    }
}

private val SampleColors = darkColorScheme(
    primary = Color(0xFF5EEAD4),
    onPrimary = Color(0xFF00201C),
    secondary = Color(0xFFFFC266),
    onSecondary = Color(0xFF2B1700),
    background = Color(0xFF0B0F17),
    onBackground = Color(0xFFF1F4F9),
    surface = Color(0xFF0B0F17),
    onSurface = Color(0xFFF1F4F9),
    surfaceVariant = Color(0xFF1B2230),
    onSurfaceVariant = Color(0xFFA6B0C3),
)

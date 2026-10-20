package io.github.halilozel1903.freetogame.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.tv.material3.MaterialTheme
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import kotlinx.coroutines.delay

/** How often a failed image is requested again before it counts as failed. */
private const val ImageRetries = 3

/**
 * An image from FreeToGame (a thumbnail or a screenshot), loaded with Coil. A plain panel stands in while it loads;
 * a failed load is retried a few times. Screenshot scenes wait until every one of these on screen has loaded, and
 * fail when one does not.
 */
@Composable
fun GameImage(url: String?, contentDescription: String?, modifier: Modifier = Modifier) {
    val monitor = LocalSceneMonitor.current
    var attempt by remember(url) { mutableIntStateOf(0) }
    var status by remember(url) { mutableStateOf(if (url.isNullOrEmpty()) ImageStatus.Failed else ImageStatus.Loading) }
    val retrying = status == ImageStatus.Failed && !url.isNullOrEmpty() && attempt < ImageRetries
    if (monitor != null) {
        val id = remember { Any() }
        DisposableEffect(monitor, id) { onDispose { monitor.image(id, null) } }
        // A failure that will be retried still counts as loading.
        LaunchedEffect(monitor, id, url) {
            snapshotFlow {
                if (status == ImageStatus.Failed && !url.isNullOrEmpty() && attempt < ImageRetries) ImageStatus.Loading else status
            }.collect { monitor.image(id, it) }
        }
    }
    if (retrying) {
        LaunchedEffect(url, attempt) {
            delay(1_000L shl attempt)
            status = ImageStatus.Loading
            attempt++
        }
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (!url.isNullOrEmpty()) {
            key(url, attempt) {
                AsyncImage(
                    model = url,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onState = { state ->
                        status = when (state) {
                            is AsyncImagePainter.State.Success -> ImageStatus.Loaded
                            is AsyncImagePainter.State.Error -> ImageStatus.Failed
                            else -> ImageStatus.Loading
                        }
                    },
                )
            }
        }
    }
}

package com.glassous.openqwens.ui.activities

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.glassous.openqwens.ui.theme.OpenQwensTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL

class MediaViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val mediaPath = intent.getStringExtra("media_path") ?: ""
        val isVideo = intent.getBooleanExtra("is_video", false)

        setContent {
            OpenQwensTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    MediaViewerScreen(
                        mediaPath = mediaPath,
                        isVideo = isVideo,
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun MediaViewerScreen(
    mediaPath: String,
    isVideo: Boolean,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isVideo) {
            VideoPlayer(mediaPath = mediaPath)
        } else {
            ImageViewer(mediaPath = mediaPath)
        }

        // Top Bar Controls
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth() // Use fillMaxWidth to position elements relative to the screen width
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Back button
            IconButton(
                onClick = onBackClick
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Right side controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Rotate Button
                IconButton(
                    onClick = {
                        val activity = context as? Activity
                        if (activity != null) {
                            if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            } else {
                                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.ScreenRotation,
                        contentDescription = "Rotate Screen",
                        tint = Color.White
                    )
                }

                // Save Button
                IconButton(
                    onClick = {
                        scope.launch {
                            saveToGallery(context, mediaPath, isVideo)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.SaveAlt,
                        contentDescription = "Save to Gallery",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

private suspend fun saveToGallery(context: Context, mediaPath: String, isVideo: Boolean) {
    withContext(Dispatchers.IO) {
        try {
            val filename = "OpenQwens_${System.currentTimeMillis()}.${if (isVideo) "mp4" else "jpg"}"
            val mimeType = if (isVideo) "video/mp4" else "image/jpeg"
            val directory = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "$directory/OpenQwens")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val collection = if (isVideo) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
            }

            val uri = resolver.insert(collection, contentValues)
            
            uri?.let { outputUri ->
                resolver.openOutputStream(outputUri)?.use { outputStream ->
                    val inputStream: InputStream = if (mediaPath.startsWith("http")) {
                        URL(mediaPath).openStream()
                    } else {
                        FileInputStream(File(mediaPath))
                    }
                    
                    inputStream.use { input ->
                        input.copyTo(outputStream)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(outputUri, contentValues, null, null)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "已保存到相册", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "保存出错: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun ImageViewer(mediaPath: String) {
    val context = LocalContext.current
    val model = remember(mediaPath) {
        if (mediaPath.startsWith("http")) {
            mediaPath
        } else {
            File(mediaPath)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(model)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(mediaPath: String) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = if (mediaPath.startsWith("http")) {
                Uri.parse(mediaPath)
            } else {
                Uri.fromFile(File(mediaPath))
            }
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

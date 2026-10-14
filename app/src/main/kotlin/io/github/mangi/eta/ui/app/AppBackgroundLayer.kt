package io.github.mangi.eta.ui.app

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import io.github.mangi.eta.data.model.MAX_BACKGROUND_DIM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 背景图最长边解码上限，避免把原图整张读进内存。 */
private const val MAX_BACKGROUND_EDGE = 2048

/**
 * 自定义背景图渲染层：铺满整屏并叠加暗化遮罩。
 *
 * - 图片按最长边采样解码，大图不会整张进内存；
 * - 未设置背景图、URI 失效或解码失败时什么都不画，自动回落到主题纯色背景。
 */
@Composable
internal fun AppBackgroundLayer(
    uriString: String?,
    dim: Float,
    modifier: Modifier = Modifier,
) {
    val bitmap = rememberBackgroundBitmap(uriString) ?: return
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        val clamped = if (dim.isFinite()) dim.coerceIn(0f, MAX_BACKGROUND_DIM) else 0f
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = clamped)),
            )
        }
    }
}

@Composable
private fun rememberBackgroundBitmap(uriString: String?): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    var bitmap by remember(uriString) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uriString) {
        bitmap = null
        val uri = uriString?.takeIf { it.isNotBlank() }?.let(Uri::parse) ?: return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) { decodeBackgroundBitmap(context, uri) }
    }
    return bitmap
}

private fun decodeBackgroundBitmap(context: Context, uri: Uri): ImageBitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        null
    } else {
        val options = BitmapFactory.Options().apply {
            inSampleSize = backgroundSampleSize(bounds.outWidth, bounds.outHeight)
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }?.asImageBitmap()
    }
}.getOrNull()

private fun backgroundSampleSize(width: Int, height: Int): Int {
    var sampleSize = 1
    var longestEdge = maxOf(width, height)
    while (longestEdge / 2 >= MAX_BACKGROUND_EDGE) {
        longestEdge /= 2
        sampleSize *= 2
    }
    return sampleSize
}

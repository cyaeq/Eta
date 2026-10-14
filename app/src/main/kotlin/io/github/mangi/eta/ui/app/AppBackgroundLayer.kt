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
 * 解码结果进程级缓存。
 *
 * 二级页要各自铺一层背景（见 [AppBackgroundFill]），如果每个页面都重新解码一次，
 * 进一次设置页就要多解一张 2048 级别的位图。缓存让所有页面共用同一份 ImageBitmap。
 */
private val backgroundBitmapCache = mutableMapOf<String, ImageBitmap>()

private fun readCachedBackgroundBitmap(key: String): ImageBitmap? =
    synchronized(backgroundBitmapCache) { backgroundBitmapCache[key] }

private fun writeCachedBackgroundBitmap(key: String, bitmap: ImageBitmap) {
    synchronized(backgroundBitmapCache) {
        // 只保留最近两张，避免切换过多壁纸后一直攥着位图不放。
        if (!backgroundBitmapCache.containsKey(key) && backgroundBitmapCache.size >= 2) {
            backgroundBitmapCache.keys.firstOrNull()?.let(backgroundBitmapCache::remove)
        }
        backgroundBitmapCache[key] = bitmap
    }
}

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

/**
 * 二级页自绘的背景底。
 *
 * NavDisplay 会保留下层 Home 页继续绘制，二级页一旦把 Scaffold 容器设成透明，
 * 透出来的是下层页面（聊天内容和侧栏），而不是壁纸。所以二级页必须自己铺一层背景，
 * 而不是指望穿透到全局背景层。图片、裁剪比例和暗化参数都与全局背景层一致，因此视觉上无缝。
 */
@Composable
internal fun AppBackgroundFill(modifier: Modifier = Modifier) {
    val appearance = LocalAppearanceSettings.current
    AppBackgroundLayer(
        uriString = appearance.backgroundImageUri,
        dim = appearance.backgroundDim,
        modifier = modifier,
    )
}

@Composable
private fun rememberBackgroundBitmap(uriString: String?): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    val key = uriString?.takeIf { it.isNotBlank() }
    var bitmap by remember(key) { mutableStateOf(key?.let(::readCachedBackgroundBitmap)) }
    LaunchedEffect(key) {
        if (key == null) {
            bitmap = null
            return@LaunchedEffect
        }
        if (bitmap == null) {
            val decoded = withContext(Dispatchers.IO) {
                decodeBackgroundBitmap(context, Uri.parse(key))
            }
            if (decoded != null) writeCachedBackgroundBitmap(key, decoded)
            bitmap = decoded
        }
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

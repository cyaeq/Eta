package io.github.mangi.eta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import io.github.mangi.eta.data.model.AppearanceTopBarBlurStyle
import io.github.mangi.eta.ui.app.LocalBlurEnabled
import io.github.mangi.eta.ui.app.LocalTopBarBlurStyle
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 顶栏用的采样层。
 *
 * [homeTransparent] 只在「首页 + 顶栏透明设置开启」时为 true：首页顶栏不铺底色也不做毛玻璃，
 * 直接透出全局壁纸层，所以连采样层都不用建，省掉一次白跑的 layer 捕获。
 *
 * 二级页必须传 false：NavDisplay 会保留下层 Home 页继续绘制，二级页顶栏一旦透出下层内容，
 * 标题就会和下层文字叠在一起。二级页恢复原有毛玻璃（运行时不支持模糊时退化为半透明底）。
 */
@Composable
internal fun rememberTopBarBackdrop(homeTransparent: Boolean = false): LayerBackdrop? {
    if (homeTransparent) return null
    if (!LocalBlurEnabled.current || !isRuntimeShaderSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/**
 * 顶栏底。
 *
 * [transparent] 为 true 时不铺任何底色，直接透出下面的内容——**只有首页该用这一档**：
 * 首页背景是全局壁纸层，透出来就是壁纸。二级页不能透明，因为 NavDisplay 保留下层 Home 页
 * 继续绘制，而二级页的 backdrop 采样的正是下层画面，透明会把下层页面的文字叠到自己身上。
 */
@Composable
internal fun TopBarBackdrop(
    backdrop: LayerBackdrop?,
    transparent: Boolean = false,
    content: @Composable () -> Unit,
) {
    val surfaceColor = MiuixTheme.colorScheme.surface
    val modifier = when {
        // 顶栏透明：不铺底色、不做毛玻璃，直接把壁纸透出来。
        transparent -> Modifier
        // 运行时不支持模糊时没有毛玻璃可依托，退化成半透明底：壁纸仍然透得出来，
        // 只是少了模糊质感。没设置背景图时 etaTranslucent 返回原色，顶栏保持不透明。
        backdrop == null -> Modifier.background(etaTranslucent(surfaceColor, TopBarFallbackSurfaceAlpha))
        LocalTopBarBlurStyle.current == AppearanceTopBarBlurStyle.PROGRESSIVE -> {
            Modifier.progressiveTextureBlur(
                backdrop = backdrop,
                shape = RectangleShape,
                gradient = ProgressiveBlur.Top.copy(curve = 2.2f),
                blurRadius = ProgressiveTopBarBlurRadius,
                colors = BlurColors(
                    blendColors = listOf(
                        BlendColorEntry(surfaceColor.copy(alpha = ProgressiveTopBarSurfaceAlpha)),
                    ),
                ),
            )
        }
        else -> {
            Modifier.textureBlur(
                backdrop = backdrop,
                shape = RectangleShape,
                blurRadius = TopBarBlurRadius,
                colors = BlurColors(
                    blendColors = listOf(
                        BlendColorEntry(surfaceColor.copy(alpha = TopBarSurfaceAlpha)),
                    ),
                ),
            )
        }
    }
    Box(modifier = modifier) { content() }
}

internal fun Modifier.captureForTopBar(backdrop: LayerBackdrop?): Modifier =
    if (backdrop == null) this else layerBackdrop(backdrop)

/**
 * 顶栏容器色。
 *
 * 底色统一由 [TopBarBackdrop] 负责绘制（毛玻璃、半透明回落或完全透明），这里始终返回透明，
 * 避免顶栏自己再铺一层把效果盖掉。
 */
@Composable
internal fun topBarContainerColor(backdrop: LayerBackdrop?): Color = Color.Transparent

private const val TopBarBlurRadius = 25f
private const val TopBarSurfaceAlpha = 0.62f
private const val TopBarFallbackSurfaceAlpha = 0.82f
private const val ProgressiveTopBarBlurRadius = 10f
private const val ProgressiveTopBarSurfaceAlpha = 0.3f

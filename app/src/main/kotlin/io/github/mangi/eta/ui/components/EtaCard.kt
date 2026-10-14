package io.github.mangi.eta.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import io.github.mangi.eta.ui.app.LocalAppearanceSettings

internal object EtaCardDefaults {
    val CornerRadius = 16.dp

    /** 设置了自定义背景图时卡片的容器色透明度：够透看到壁纸，又不至于压不住正文。 */
    const val BackgroundCardAlpha = 0.72f
}

/**
 * 把不透明容器色按自定义背景图的情况转成半透明。
 *
 * 没设置背景图时原样返回，行为与改动前完全一致；设置了背景图时按 [alpha] 透出壁纸。
 * 全站的"灰色方块盖死壁纸"问题都从这里统一收敛。
 */
@Composable
internal fun etaTranslucent(color: Color, alpha: Float = EtaCardDefaults.BackgroundCardAlpha): Color =
    if (LocalAppearanceSettings.current.hasBackgroundImage) color.copy(alpha = alpha) else color

/** 分组卡片 / 长列表分组的容器底色。 */
@Composable
internal fun etaGroupContainerColor(): Color =
    etaTranslucent(MiuixTheme.colorScheme.surfaceContainer)

/** 卡片底色，供 [EtaCard] 的默认参数使用。 */
@Composable
internal fun etaCardColors(): CardColors =
    CardDefaults.defaultColors(color = etaGroupContainerColor())

@Composable
internal fun EtaCard(
    modifier: Modifier = Modifier,
    insideMargin: PaddingValues = CardDefaults.InsideMargin,
    colors: CardColors = etaCardColors(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        cornerRadius = EtaCardDefaults.CornerRadius,
        insideMargin = insideMargin,
        colors = colors,
        content = content,
    )
}

@Composable
internal fun EtaCard(
    modifier: Modifier = Modifier,
    insideMargin: PaddingValues = CardDefaults.InsideMargin,
    colors: CardColors = etaCardColors(),
    pressFeedbackType: PressFeedbackType = PressFeedbackType.None,
    showIndication: Boolean = false,
    holdDownState: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        cornerRadius = EtaCardDefaults.CornerRadius,
        insideMargin = insideMargin,
        colors = colors,
        pressFeedbackType = pressFeedbackType,
        showIndication = showIndication,
        holdDownState = holdDownState,
        onClick = onClick,
        onLongPress = onLongPress,
        content = content,
    )
}

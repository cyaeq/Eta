package io.github.mangi.eta.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.ui.app.LocalAppearanceSettings
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal object EtaPreferenceColors {
    /** 主题强调色，日间模式下用来补 Miuix 缺失的主色，不参与列表图标着色。 */
    val Accent = Color(0xFF0080FF)

    /**
     * 列表项的装饰色。
     *
     * 统一收敛成中性色：一屏设置项里出现五六种高饱和色块会显得很碎，
     * 只保留 [Green] / [Yellow] 这类有状态含义的颜色。想回退成彩色就改回具体色值。
     */
    val Blue: Color
        @Composable get() = MiuixTheme.colorScheme.onSurfaceVariantActions

    val Green = StatusSuccess

    val Orange: Color
        @Composable get() = MiuixTheme.colorScheme.onSurfaceVariantActions

    val Yellow = StatusWarning
}

internal object EtaPreferenceDefaults {
    val SidePadding = 16.dp
    val GroupSpacing = 16.dp
    val IconSize = 24.dp
    val IconTextGap = 16.dp
    val ContentStart = SidePadding + IconSize + IconTextGap
    fun contentStart(hasLeading: Boolean) = if (hasLeading) ContentStart else SidePadding
    val RowMinHeight = 52.dp
}

@Composable
internal fun EtaPreferenceTheme(content: @Composable () -> Unit) {
    val colors = MiuixTheme.colorScheme
    val appearance = LocalAppearanceSettings.current
    val pageColors = if (!appearance.monetEnabled && colors.background.luminance() > 0.5f) {
        colors.copy(background = Color(0xFFF0F1F2), primary = EtaPreferenceColors.Accent)
    } else {
        colors
    }
    MiuixTheme(colors = pageColors, content = content)
}

@Composable
internal fun EtaPreferenceGroup(
    modifier: Modifier = Modifier.padding(horizontal = EtaPreferenceDefaults.SidePadding).padding(bottom = EtaPreferenceDefaults.GroupSpacing),
    insideMargin: PaddingValues = PaddingValues(0.dp),
    colors: CardColors = etaCardColors(),
    content: @Composable ColumnScope.() -> Unit,
) {
    EtaCard(modifier = modifier, insideMargin = insideMargin, colors = colors, content = content)
}

@Composable
internal fun EtaPreferenceGroupTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.subtitle,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = modifier.padding(start = 32.dp, end = 32.dp, top = 4.dp, bottom = 8.dp),
    )
}

@Composable
internal fun EtaPreferenceIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MiuixTheme.colorScheme.onSurfaceVariantActions,
    enabled: Boolean = true,
) {
    // 满幅轮廓稍作光学校正，但所有图标都占相同宽度，保证正文和分割线对齐。
    val glyphSize = when (icon) {
        Icons.Rounded.Extension, Icons.Rounded.TheaterComedy, Icons.AutoMirrored.Rounded.MenuBook -> 22.dp
        else -> EtaPreferenceDefaults.IconSize
    }
    Box(modifier.size(EtaPreferenceDefaults.IconSize), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(glyphSize),
            tint = if (enabled) tint else MiuixTheme.colorScheme.disabledOnSurface,
        )
    }
}

@Composable
internal fun EtaPreferenceDivider(hasLeading: Boolean = true, modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(start = EtaPreferenceDefaults.contentStart(hasLeading), end = EtaPreferenceDefaults.SidePadding),
        thickness = 0.33.dp,
        color = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.1f),
    )
}

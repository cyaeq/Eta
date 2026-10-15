package io.github.mangi.eta.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import io.github.mangi.eta.ui.layout.rememberIsWideScreen
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 宽屏固定小标题；手机按 [EtaControlDefaults.CompactTopBarOnPhone] 决定用紧凑标题还是可折叠大标题。 */
@Composable
fun AdaptiveTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.surface,
    scrollBehavior: ScrollBehavior? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
) {
    EtaTopBarTitleStyle {
        if (rememberIsWideScreen() || EtaControlDefaults.CompactTopBarOnPhone) {
            SmallTopAppBar(
                title = title,
                modifier = modifier,
                color = color,
                scrollBehavior = scrollBehavior,
                navigationIcon = navigationIcon,
                actions = actions,
                bottomContent = bottomContent,
            )
        } else {
            TopAppBar(
                title = title,
                modifier = modifier,
                color = color,
                scrollBehavior = scrollBehavior,
                navigationIcon = navigationIcon,
                actions = actions,
                bottomContent = bottomContent,
            )
        }
    }
}

/**
 * 顶栏标题字号。
 *
 * miuix 的顶栏标题字号写死取 `textStyles.title3`（默认 20sp），在 363dp 宽的手机上偏大，
 * 与行内的工具档、状态胶囊相比明显压不住；这里收到 17sp，接近系统顶栏的观感。
 */
private val TopBarTitleFontSize = 17.sp

/**
 * 把顶栏标题的字号改小后再绘制 [content]。
 *
 * `SmallTopAppBar` / `TopAppBar` 内部直接写 `fontSize = MiuixTheme.textStyles.title3.fontSize`，
 * 对外只有 title/navigationIcon/actions，没有标题样式参数；这里借 [MiuixTheme] 重新下发一份
 * 只改了 title3 的 TextStyles，作用域仅限顶栏子树，应用里其他组件用到的 title3 不受影响。
 */
@Composable
fun EtaTopBarTitleStyle(content: @Composable () -> Unit) {
    val textStyles = MiuixTheme.textStyles
    val compactTextStyles = remember(textStyles) {
        textStyles.copy(title3 = textStyles.title3.copy(fontSize = TopBarTitleFontSize))
    }
    MiuixTheme(
        colors = MiuixTheme.colorScheme,
        textStyles = compactTextStyles,
        content = content,
    )
}

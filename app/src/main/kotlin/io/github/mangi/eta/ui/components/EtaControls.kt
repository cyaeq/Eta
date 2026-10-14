package io.github.mangi.eta.ui.components

import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextButtonColors
import top.yukonga.miuix.kmp.layout.DialogDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 全局控件的尺寸令牌。改外观时优先动这里，避免各处散改。
 *
 * 对话框圆角刻意**不在这里定义**：Miuix 会按屏幕实际圆角自适应
 * （手机端取 `clamp(屏幕圆角 - 外侧边距, 32dp, 48dp)`，大屏居中弹窗才用固定值）。
 * 之前 App 各处显式传 `cornerRadius = DialogDefaults.CornerRadius` 把这个自适应覆盖掉了，
 * 于是手机底部弹出层的顶部圆角偏小、和屏幕底部圆角对不上，已统一移除。
 */
internal object EtaControlDefaults {
    val ButtonCornerRadius = 22.dp
    val ButtonMinHeight = 44.dp
    val ButtonPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    val DialogInsideMargin = DpSize(24.dp, 24.dp)
    val DialogOutsideMargin = DpSize(16.dp, 16.dp)

    /**
     * 弹窗呈现方式，改这一个值就能全局切换：
     * - true：居中卡片（Material 3 / RikkaHub 那种，缩放入场）
     * - false：Miuix 默认的贴底弹出层（贴底滑入，圆角跟随屏幕）
     */
    val DialogCentered = true

    /**
     * 手机上是否使用紧凑标题栏。
     * - true：标题固定成一行（RikkaHub 那种），不随滚动折叠成超大号
     * - false：Miuix 默认的大标题，展开时占掉接近四分之一屏
     */
    val CompactTopBarOnPhone = true
}

@Composable
internal fun EtaTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cornerRadius: Dp = EtaControlDefaults.ButtonCornerRadius,
    minWidth: Dp = ButtonDefaults.MinWidth,
    minHeight: Dp = EtaControlDefaults.ButtonMinHeight,
    colors: TextButtonColors = ButtonDefaults.textButtonColors(),
    insideMargin: PaddingValues = EtaControlDefaults.ButtonPadding,
    textStyle: TextStyle? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = LocalIndication.current,
) = TextButton(
    text = text, onClick = onClick, modifier = modifier, enabled = enabled,
    cornerRadius = cornerRadius, minWidth = minWidth, minHeight = minHeight,
    colors = colors, insideMargin = insideMargin, textStyle = textStyle,
    interactionSource = interactionSource, indication = indication,
)

@Composable
internal fun EtaWindowDialog(
    show: Boolean,
    title: String? = null,
    summary: String? = null,
    modifier: Modifier = Modifier,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    EtaPreferenceTheme {
        WindowDialog(
            show = show, title = title, summary = summary, modifier = modifier,
            largeScreen = EtaControlDefaults.DialogCentered,
            backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh,
            onDismissRequest = onDismissRequest, onDismissFinished = onDismissFinished,
            insideMargin = EtaControlDefaults.DialogInsideMargin,
            outsideMargin = EtaControlDefaults.DialogOutsideMargin,
            maxWidth = DialogDefaults.MaxWidth,
            content = content,
        )
    }
}

@Composable
internal fun EtaOverlayDialog(
    show: Boolean,
    title: String? = null,
    summary: String? = null,
    modifier: Modifier = Modifier,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    EtaPreferenceTheme {
        OverlayDialog(
            show = show, title = title, summary = summary, modifier = modifier,
            largeScreen = EtaControlDefaults.DialogCentered,
            backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh,
            onDismissRequest = onDismissRequest, onDismissFinished = onDismissFinished,
            insideMargin = EtaControlDefaults.DialogInsideMargin,
            outsideMargin = EtaControlDefaults.DialogOutsideMargin,
            maxWidth = DialogDefaults.MaxWidth,
            content = content,
        )
    }
}

@Composable
internal fun EtaSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = Switch(
    checked = checked, onCheckedChange = onCheckedChange, enabled = enabled,
    modifier = modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        val width = 44.dp.roundToPx()
        val height = 24.dp.roundToPx()
        layout(width, height) {
            placeable.placeWithLayer((width - placeable.width) / 2, (height - placeable.height) / 2) {
                scaleX = width.toFloat() / placeable.width
                scaleY = height.toFloat() / placeable.height
            }
        }
    },
)

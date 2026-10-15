package io.github.mangi.eta.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.R
import io.github.mangi.eta.ui.app.LocalAppearanceSettings
import top.yukonga.miuix.kmp.basic.Icon

/** 单个快捷图标的触摸区与外框尺寸；比顶栏常规按钮小一圈，四个并排也放得下。 */
private val QuickActionTouchSize = 24.dp

/** 图标本体尺寸。 */
private val QuickActionGlyphSize = 17.dp

/**
 * 顶部胶囊行左侧的快捷入口：虚拟屏、分类、终端、浏览器。
 *
 * 从「⋯」菜单里挪出来，缩成四个小图标，点一下直达，不用先展开菜单。
 * 是否启用由外观设置里的 `quickActionsInStatusRow` 控制；关掉时不绘制任何内容，
 * 调用方可以无条件把它塞进行首。
 */
@Composable
internal fun HomeQuickActions(
    onOpenCategories: () -> Unit,
    onOpenTerminal: () -> Unit,
    onOpenBrowser: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!LocalAppearanceSettings.current.quickActionsInStatusRow) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 首项固定是「打开虚拟屏」，图标可能为空（没有收藏应用时用默认图标兜底）。
    val virtualEntry = rememberVirtualScreenMenuEntries().firstOrNull()
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        QuickAction(
            label = virtualEntry?.label ?: stringResource(R.string.virtual_screen_title),
            onClick = { openVirtualScreen(context, scope, virtualEntry?.component) },
        ) { iconModifier ->
            val bitmap = virtualEntry?.icon
            if (bitmap != null) {
                ShortcutIcon(bitmap, iconModifier)
            } else {
                Icon(
                    imageVector = Icons.Rounded.Smartphone,
                    contentDescription = null,
                    modifier = iconModifier,
                )
            }
        }
        QuickAction(
            label = stringResource(R.string.sidebar_categories),
            onClick = onOpenCategories,
        ) { iconModifier ->
            Icon(
                imageVector = Icons.Rounded.Folder,
                contentDescription = null,
                modifier = iconModifier,
            )
        }
        QuickAction(
            label = stringResource(R.string.action_open_terminal),
            onClick = onOpenTerminal,
        ) { iconModifier ->
            Icon(
                imageVector = Icons.Rounded.Terminal,
                contentDescription = null,
                modifier = iconModifier,
            )
        }
        QuickAction(
            label = stringResource(R.string.action_open_browser),
            onClick = onOpenBrowser,
        ) { iconModifier ->
            Icon(
                imageVector = Icons.Rounded.Language,
                contentDescription = null,
                modifier = iconModifier,
            )
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    onClick: () -> Unit,
    icon: @Composable (Modifier) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(QuickActionTouchSize)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        icon(Modifier.size(QuickActionGlyphSize))
    }
}

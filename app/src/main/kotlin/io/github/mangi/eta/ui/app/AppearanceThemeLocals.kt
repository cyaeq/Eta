package io.github.mangi.eta.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import io.github.mangi.eta.data.model.AppearanceSettings
import io.github.mangi.eta.data.model.AppearanceTopBarBlurStyle
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val LocalAppearanceSettings = staticCompositionLocalOf { AppearanceSettings() }
internal val LocalBlurEnabled = staticCompositionLocalOf { true }
internal val LocalTopBarBlurStyle = staticCompositionLocalOf { AppearanceTopBarBlurStyle.GAUSSIAN }
internal val LocalPlatformDensity = staticCompositionLocalOf<Density?> { null }

/**
 * 页面级容器的底色。
 *
 * Miuix 的 `Scaffold` 默认铺 `surface` 色，会把 [AppBackgroundLayer] 的自定义背景图整块盖住，
 * 所以设置了背景图时改用透明，让底图透出来；没设置时保持原来的 `surface`，行为不变。
 */
@Composable
internal fun pageContainerColor(): Color =
    if (LocalAppearanceSettings.current.hasBackgroundImage) {
        Color.Transparent
    } else {
        MiuixTheme.colorScheme.surface
    }

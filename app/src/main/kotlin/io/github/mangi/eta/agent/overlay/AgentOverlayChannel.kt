package io.github.mangi.eta.agent.overlay

import android.content.Context
import android.provider.Settings
import android.view.WindowManager
import io.github.mangi.eta.agent.accessibility.AgentAccessibilityService

/**
 * 浮层窗口通道：Eta 的浮窗有两条实现路径，这里决定用哪一条。
 *
 * - 无障碍通道：借 [AgentAccessibilityService] 的 context 创建 `TYPE_ACCESSIBILITY_OVERLAY` 窗口。
 *   不依赖 `SYSTEM_ALERT_WINDOW`，只要无障碍服务在线就一定能弹出来（连"显示在其他应用上层"
 *   关着也照弹），而且截图时会被 `filterValidWindows` 过滤掉，对 Agent 自己透明。
 * - 普通通道：`TYPE_APPLICATION_OVERLAY`，必须拿到「显示在其他应用上层」权限，否则 addView 抛异常。
 *
 * 设置项「无障碍浮窗通道」（Settings.overlayAccessibilityOverlayEnabled）
 * 关掉后只走普通通道：没有悬浮窗权限就不显示浮窗，不再靠无障碍权限强行弹出。
 *
 * 缓存值由设置界面写入时同步、由 Runtime 每次要建窗口时从设置读一次，未读到过时按默认（允许）处理。
 */
internal object AgentOverlayChannel {

    /** 无障碍通道是否允许；null 表示本次进程还没读到过设置。 */
    @Volatile
    private var accessibilityOverlayEnabled: Boolean? = null

    /** 设置项变化时同步缓存，省掉一次磁盘读。 */
    fun onSettingChanged(enabled: Boolean) {
        accessibilityOverlayEnabled = enabled
    }

    fun isAccessibilityOverlayEnabled(): Boolean = accessibilityOverlayEnabled != false

    /** 这次建窗口是否真的走无障碍叠加窗口：开关允许且无障碍服务在线。 */
    fun usesAccessibilityOverlay(): Boolean =
        isAccessibilityOverlayEnabled() && AgentAccessibilityService.current() != null

    /** 当前该用的窗口类型。 */
    fun windowType(): Int = if (usesAccessibilityOverlay()) {
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
    } else {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    }

    /** 无障碍叠加窗口只能用无障碍服务自己的 context 创建，否则 addView 报 BadTokenException。 */
    fun windowContext(fallback: Context): Context =
        if (usesAccessibilityOverlay()) AgentAccessibilityService.current() ?: fallback else fallback

    /** 当前配置下能不能显示浮窗：走无障碍通道可以，走普通通道要有悬浮窗权限。 */
    fun canShowOverlay(context: Context): Boolean =
        usesAccessibilityOverlay() || Settings.canDrawOverlays(context)
}

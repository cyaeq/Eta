package io.github.mangi.eta.agent.display

import android.os.SystemClock
import android.view.InputDevice
import android.view.InputEvent
import android.view.MotionEvent

/**
 * 在 Root 辅助进程内直接注入触摸事件。
 *
 * 以前每条手势都要启动一次 /system/bin/input（app_process 冷启动，几百毫秒），
 * 这是手动滑动不跟手的主因；这里改成常驻进程内注入，并把拖动拆成 ~80Hz 的
 * MOVE 事件流，首帧 DOWN 注入成功后立刻返回，后续在后台线程继续。
 * 注入不可用时返回 false，由调用方回退到 /system/bin/input。
 */
internal class RootTouchInjector {
    private val inputManager: Any? = runCatching {
        Class.forName("android.hardware.input.InputManager").getMethod("getInstance").invoke(null)
    }.getOrNull()

    private val injectMethod: java.lang.reflect.Method? = runCatching {
        Class.forName("android.hardware.input.InputManager")
            .getMethod("injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType)
    }.getOrNull()

    private val setDisplayId: java.lang.reflect.Method? = runCatching {
        InputEvent::class.java.getMethod("setDisplayId", Int::class.javaPrimitiveType)
    }.getOrNull()

    private fun usable() = inputManager != null && injectMethod != null && setDisplayId != null

    /** 进行中手势的状态；down/move/up 分三条指令到达，所以要跨请求保留。 */
    private val gestureLock = Any()
    private var gestureDownTime = 0L
    private var gestureDisplayId = INVALID_DISPLAY

    /**
     * 分段手势：按 down → move… → up 的指令序列注入，供虚拟屏查看页的触控直通使用。
     *
     * 三条指令是分开到达的，所以进行中的手势状态（按下时间与目标显示）必须跨请求保留；
     * 目标显示与上次按下不一致时放弃注入，避免把半截手势丢到别的屏幕。
     */
    fun down(displayId: Int, x: Float, y: Float): Boolean {
        if (!usable()) return false
        val now = SystemClock.uptimeMillis()
        if (!send(displayId, event(now, now, MotionEvent.ACTION_DOWN, x, y))) return false
        synchronized(gestureLock) {
            gestureDownTime = now
            gestureDisplayId = displayId
        }
        return true
    }

    /** 没有进行中的手势（或目标显示对不上）时返回 false，由调用方决定是否回退。 */
    fun move(displayId: Int, x: Float, y: Float): Boolean {
        if (!usable()) return false
        val downTime = synchronized(gestureLock) {
            if (gestureDisplayId != displayId) return false
            gestureDownTime
        }
        val now = SystemClock.uptimeMillis()
        return send(displayId, event(downTime, now, MotionEvent.ACTION_MOVE, x, y))
    }

    /** cancel = true 发 CANCEL，用于手势被系统或查看页打断的场景。 */
    fun up(displayId: Int, x: Float, y: Float, cancel: Boolean = false): Boolean {
        if (!usable()) return false
        val now = SystemClock.uptimeMillis()
        val downTime = synchronized(gestureLock) {
            val active = if (gestureDisplayId == displayId) gestureDownTime else now
            gestureDownTime = 0L
            gestureDisplayId = INVALID_DISPLAY
            active
        }
        val action = if (cancel) MotionEvent.ACTION_CANCEL else MotionEvent.ACTION_UP
        return send(displayId, event(downTime, now, action, x, y))
    }

    /** true 表示手势已接管（DOWN 已注入，其余在后台线程）。 */
    fun startTap(displayId: Int, x: Float, y: Float): Boolean {
        if (!usable()) return false
        val now = SystemClock.uptimeMillis()
        if (!send(displayId, event(now, now, MotionEvent.ACTION_DOWN, x, y))) return false
        background {
            runCatching {
                SystemClock.sleep(45)
                send(displayId, event(now, now + 45, MotionEvent.ACTION_UP, x, y))
            }
        }
        return true
    }

    /** true 表示手势已接管；durationMs 内按 ~80Hz 补 MOVE，拖动才有连续反馈。 */
    fun startSwipe(displayId: Int, x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long): Boolean {
        if (!usable()) return false
        val duration = durationMs.coerceIn(1L, 3000L)
        val start = SystemClock.uptimeMillis()
        if (!send(displayId, event(start, start, MotionEvent.ACTION_DOWN, x1, y1))) return false
        background {
            runCatching {
                val steps = (duration / 12L).coerceIn(1L, 150L).toInt()
                var moved = true
                for (step in 1..steps) {
                    val ratio = step.toFloat() / steps
                    moved = send(
                        displayId,
                        event(start, start + (duration * ratio).toLong(), MotionEvent.ACTION_MOVE,
                            x1 + (x2 - x1) * ratio, y1 + (y2 - y1) * ratio),
                    )
                    if (!moved) break
                }
                // MOVE 中途失败时发 CANCEL，避免残留下压点。
                send(displayId, event(start, start + duration, if (moved) MotionEvent.ACTION_UP else MotionEvent.ACTION_CANCEL, x2, y2))
            }
        }
        return true
    }

    private fun background(block: () -> Unit) {
        Thread(block, "eta-touch-inject").apply { isDaemon = true; start() }
    }

    private fun event(downTime: Long, eventTime: Long, action: Int, x: Float, y: Float): MotionEvent =
        MotionEvent.obtain(downTime, eventTime, action, x, y, 0).apply {
            source = InputDevice.SOURCE_TOUCHSCREEN
        }

    private fun send(displayId: Int, event: MotionEvent): Boolean = try {
        setDisplayId?.invoke(event, displayId)
        (injectMethod?.invoke(inputManager, event, INJECT_ASYNC) as? Boolean) == true
    } catch (_: Throwable) {
        false
    } finally {
        event.recycle()
    }

    private companion object {
        /** InputManager.INJECT_INPUT_EVENT_MODE_ASYNC */
        const val INJECT_ASYNC = 0

        /** 没有任何进行中手势时的目标显示占位值。 */
        const val INVALID_DISPLAY = Int.MIN_VALUE
    }
}

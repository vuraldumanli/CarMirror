package com.vural.carmirror

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PointF
import android.view.accessibility.AccessibilityEvent

/** Araçtan gelen dokunuşları telefonda hareket olarak uygular. */
class TouchService : AccessibilityService() {

    override fun onServiceConnected() {
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    fun tap(p: PointF) {
        val path = Path().apply { moveTo(p.x, p.y) }
        dispatch(GestureDescription.StrokeDescription(path, 0, 50))
    }

    fun doubleTap(p: PointF) {
        val a = Path().apply { moveTo(p.x, p.y) }
        val b = Path().apply { moveTo(p.x, p.y) }
        dispatch(
            GestureDescription.StrokeDescription(a, 0, 40),
            GestureDescription.StrokeDescription(b, 120, 40)
        )
    }

    fun swipe(from: PointF, to: PointF, durationMs: Long) {
        val path = Path().apply {
            moveTo(from.x, from.y)
            lineTo(to.x, to.y)
        }
        dispatch(GestureDescription.StrokeDescription(path, 0, durationMs))
    }

    fun pinch(focus: PointF, startGap: Float, endGap: Float, maxX: Float) {
        fun clampX(v: Float) = v.coerceIn(1f, maxX - 1f)
        val left = Path().apply {
            moveTo(clampX(focus.x - startGap), focus.y)
            lineTo(clampX(focus.x - endGap), focus.y)
        }
        val right = Path().apply {
            moveTo(clampX(focus.x + startGap), focus.y)
            lineTo(clampX(focus.x + endGap), focus.y)
        }
        dispatch(
            GestureDescription.StrokeDescription(left, 0, 300),
            GestureDescription.StrokeDescription(right, 0, 300)
        )
    }

    private fun dispatch(vararg strokes: GestureDescription.StrokeDescription) {
        val b = GestureDescription.Builder()
        strokes.forEach { b.addStroke(it) }
        dispatchGesture(b.build(), null, null)
    }

    companion object {
        @Volatile
        var instance: TouchService? = null
            private set

        val isEnabled: Boolean get() = instance != null

        fun global(action: Int) {
            instance?.performGlobalAction(action)
        }
    }
}

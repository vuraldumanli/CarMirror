package com.vural.carmirror

import android.content.Context
import android.graphics.Point
import android.graphics.PointF
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.Surface

/**
 * Telefon ekran yakalaması (MediaProjection) ile araç ekranındaki yüzey arasındaki köprü.
 * Telefon görüntüsü doğrudan Android Auto yüzeyine yazılır; ara kopya yok, gecikme düşük.
 */
object MirrorBridge {
    private const val TAG = "MirrorBridge"

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var carSurface: Surface? = null
    private var carW = 800
    private var carH = 480
    private var carDpi = 160

    var onStopped: (() -> Unit)? = null

    val isProjecting: Boolean get() = projection != null
    val hasCarSurface: Boolean get() = carSurface != null

    @Synchronized
    fun start(p: MediaProjection) {
        stop()
        projection = p
        // Android 14+: createVirtualDisplay'den önce callback kaydı zorunlu
        p.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Log.i(TAG, "projection stopped by system")
                release()
            }
        }, Handler(Looper.getMainLooper()))
        // Android 14+ bir token ile yalnızca bir kez createVirtualDisplay çağrılabilir,
        // bu yüzden hemen oluşturup yüzey değiştikçe resize/setSurface kullanıyoruz.
        virtualDisplay = p.createVirtualDisplay(
            "CarMirror", carW, carH, carDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            carSurface, null, null
        )
    }

    @Synchronized
    fun attachSurface(surface: Surface, width: Int, height: Int, dpi: Int) {
        carSurface = surface
        carW = width
        carH = height
        carDpi = dpi
        virtualDisplay?.let {
            it.resize(width, height, dpi)
            it.surface = surface
        }
    }

    @Synchronized
    fun detachSurface(surface: Surface?) {
        if (surface == null || surface == carSurface) {
            carSurface = null
            virtualDisplay?.surface = null
        }
    }

    @Synchronized
    fun stop() {
        val p = projection
        release()
        p?.stop()
    }

    @Synchronized
    private fun release() {
        val wasRunning = projection != null
        virtualDisplay?.release()
        virtualDisplay = null
        projection = null
        if (wasRunning) onStopped?.invoke()
    }

    fun phoneSize(context: Context): Point {
        val dm = context.getSystemService(DisplayManager::class.java)
        val p = Point()
        @Suppress("DEPRECATION")
        dm.getDisplay(Display.DEFAULT_DISPLAY).getRealSize(p)
        return p
    }

    /** Ayna içerik, araç yüzeyine en-boy oranı korunarak ortalanmış şekilde sığdırılır. */
    fun scale(context: Context): Float {
        val ps = phoneSize(context)
        return minOf(carW.toFloat() / ps.x, carH.toFloat() / ps.y)
    }

    /** Araç ekranı koordinatını telefon koordinatına çevirir; siyah boşluğa basıldıysa null. */
    fun carToPhone(context: Context, x: Float, y: Float): PointF? {
        val ps = phoneSize(context)
        val s = minOf(carW.toFloat() / ps.x, carH.toFloat() / ps.y)
        val offX = (carW - ps.x * s) / 2f
        val offY = (carH - ps.y * s) / 2f
        val px = (x - offX) / s
        val py = (y - offY) / s
        if (px < 0 || py < 0 || px >= ps.x || py >= ps.y) return null
        return PointF(px, py)
    }
}

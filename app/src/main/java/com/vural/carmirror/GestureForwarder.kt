package com.vural.carmirror

import android.content.Context
import android.graphics.PointF
import android.os.Handler
import android.os.Looper

/**
 * Android Auto yüzey olaylarını (tıklama, kaydırma, fırlatma, yakınlaştırma)
 * telefon hareketlerine çevirir. Android Auto kaydırmayı küçük parçalar halinde
 * gönderdiği için parçalar biriktirilip tek bir kaydırma hareketi olarak uygulanır.
 */
class GestureForwarder(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())

    // Parmağın araç ekranındaki toplam hareketi (piksel)
    private var moveX = 0f
    private var moveY = 0f

    private var scaleAcc = 1f
    private var scaleFocus = PointF()

    private val flushScroll = Runnable { sendSwipe(250) }
    private val flushScale = Runnable { sendPinch() }

    fun click(x: Float, y: Float) {
        val svc = TouchService.instance ?: return
        val p = MirrorBridge.carToPhone(context, x, y) ?: return
        svc.tap(p)
    }

    fun scroll(distanceX: Float, distanceY: Float) {
        // distance = önceki konum - yeni konum (GestureDetector kuralı)
        moveX -= distanceX
        moveY -= distanceY
        handler.removeCallbacks(flushScroll)
        handler.postDelayed(flushScroll, 120)
    }

    fun fling(velocityX: Float, velocityY: Float) {
        handler.removeCallbacks(flushScroll)
        moveX += velocityX * 0.08f
        moveY += velocityY * 0.08f
        sendSwipe(90)
    }

    fun scale(focusX: Float, focusY: Float, factor: Float) {
        if (factor < 0f) {
            // Android Auto çift dokunmayı negatif ölçekle bildirir
            val svc = TouchService.instance ?: return
            MirrorBridge.carToPhone(context, focusX, focusY)?.let { svc.doubleTap(it) }
            return
        }
        scaleAcc *= factor
        scaleFocus = PointF(focusX, focusY)
        handler.removeCallbacks(flushScale)
        handler.postDelayed(flushScale, 150)
    }

    private fun sendSwipe(durationMs: Long) {
        val dx = moveX
        val dy = moveY
        moveX = 0f
        moveY = 0f
        val svc = TouchService.instance ?: return
        val s = MirrorBridge.scale(context)
        if (s <= 0f) return
        val ps = MirrorBridge.phoneSize(context)
        // Telefon koordinatında hareket; ekranın ortasından simetrik çekilir
        val px = (dx / s).coerceIn(-ps.x * 0.9f, ps.x * 0.9f)
        val py = (dy / s).coerceIn(-ps.y * 0.9f, ps.y * 0.9f)
        if (px * px + py * py < 16f) return
        val cx = ps.x / 2f
        val cy = ps.y / 2f
        svc.swipe(
            PointF(cx - px / 2f, cy - py / 2f),
            PointF(cx + px / 2f, cy + py / 2f),
            durationMs
        )
    }

    private fun sendPinch() {
        val f = scaleAcc.coerceIn(0.3f, 3f)
        scaleAcc = 1f
        val svc = TouchService.instance ?: return
        val focus = MirrorBridge.carToPhone(context, scaleFocus.x, scaleFocus.y) ?: return
        val ps = MirrorBridge.phoneSize(context)
        val base = ps.x * 0.12f
        if (f >= 1f) svc.pinch(focus, base, base * f, ps.x.toFloat())
        else svc.pinch(focus, base / f, base, ps.x.toFloat())
    }

    fun cancel() {
        handler.removeCallbacksAndMessages(null)
    }
}

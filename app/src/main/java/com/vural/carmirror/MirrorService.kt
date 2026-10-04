package com.vural.carmirror

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.os.PowerManager

/** MediaProjection'ı tutan ön plan servisi. Telefon ekranını açık tutar, yoksa ayna kararır. */
class MirrorService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            MirrorBridge.stop()
            stopSelf()
            return START_NOT_STICKY
        }

        val code = intent?.getIntExtra(EXTRA_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        @Suppress("DEPRECATION")
        val data: Intent? = intent?.getParcelableExtra(EXTRA_DATA)
        if (code != Activity.RESULT_OK || data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Android 14: getMediaProjection'dan önce mediaProjection tipinde ön plana geçilmeli
        startForeground(
            Notifications.ID_RUN,
            Notifications.running(this),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )

        val mpm = getSystemService(MediaProjectionManager::class.java)
        val projection = mpm.getMediaProjection(code, data)
        if (projection == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        MirrorBridge.onStopped = { stopSelf() }
        MirrorBridge.start(projection)
        Notifications.cancelAsk(this)

        @Suppress("DEPRECATION")
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "CarMirror:screen"
            ).apply { acquire(6 * 60 * 60 * 1000L) }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        MirrorBridge.onStopped = null
        MirrorBridge.stop()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.vural.carmirror.STOP"
        const val EXTRA_CODE = "code"
        const val EXTRA_DATA = "data"
    }
}

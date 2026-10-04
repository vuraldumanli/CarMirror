package com.vural.carmirror

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object Notifications {
    const val CH_RUN = "mirror_running"
    const val CH_ASK = "mirror_ask"
    const val ID_RUN = 1
    const val ID_ASK = 2

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_RUN, "Yansıtma çalışıyor", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_ASK, "Yansıtmayı başlat", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    private fun openApp(ctx: Context) = PendingIntent.getActivity(
        ctx, 0,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    fun running(ctx: Context): Notification {
        ensureChannels(ctx)
        val stop = PendingIntent.getService(
            ctx, 1,
            Intent(ctx, MirrorService::class.java).setAction(MirrorService.ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(ctx, CH_RUN)
            .setSmallIcon(R.drawable.ic_app)
            .setContentTitle("Ekran araca yansıtılıyor")
            .setContentIntent(openApp(ctx))
            .addAction(Notification.Action.Builder(null, "Durdur", stop).build())
            .setOngoing(true)
            .build()
    }

    fun askToStart(ctx: Context) {
        ensureChannels(ctx)
        val n = Notification.Builder(ctx, CH_ASK)
            .setSmallIcon(R.drawable.ic_app)
            .setContentTitle("Araç bağlandı")
            .setContentText("Ekranı yansıtmak için dokunun ve Başlat'a basın")
            .setContentIntent(openApp(ctx))
            .setAutoCancel(true)
            .build()
        try {
            ctx.getSystemService(NotificationManager::class.java).notify(ID_ASK, n)
        } catch (_: SecurityException) {
        }
    }

    fun cancelAsk(ctx: Context) {
        ctx.getSystemService(NotificationManager::class.java).cancel(ID_ASK)
    }
}

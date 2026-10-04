package com.vural.carmirror

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }
        status = TextView(this).apply { textSize = 16f; setPadding(0, 0, 0, pad) }
        root.addView(status)

        fun button(text: String, onClick: () -> Unit) = root.addView(Button(this).apply {
            this.text = text
            setOnClickListener { onClick() }
        })

        button("1. Dokunma iznini aç (Erişilebilirlik)") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        button("2. Pil kısıtlamasını kaldır") {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:$packageName"))
            )
        }
        button("Yansıtmayı BAŞLAT") { requestProjection() }
        button("Durdur") {
            startService(Intent(this, MirrorService::class.java).setAction(MirrorService.ACTION_STOP))
            refresh()
        }

        root.addView(TextView(this).apply {
            textSize = 14f
            gravity = Gravity.START
            setPadding(0, pad, 0, 0)
            text = """
                Kurulum (bir kez):
                • Android Auto ayarlarında sürüm numarasına 10 kez dokunup geliştirici modunu açın.
                • Sağ üst menü > Geliştirici ayarları > "Bilinmeyen kaynaklar" işaretleyin.
                • Yukarıdaki 1 ve 2 numaralı izinleri verin.

                Her sürüşte:
                • Telefonu araca bağlayın, burada BAŞLAT'a basın, çıkan pencerede "Tüm ekran"ı seçin.
                • Araç ekranında Android Auto menüsünden "Ekran Yansıt"ı açın.
                • Üstteki Geri / Ana / Son düğmeleri telefonun tuşları gibi çalışır.
            """.trimIndent()
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val pm = getSystemService(PowerManager::class.java)
        status.text = buildString {
            append("Dokunma izni: ").append(if (TouchService.isEnabled) "AÇIK" else "KAPALI").append('\n')
            append("Pil kısıtlaması: ")
                .append(if (pm.isIgnoringBatteryOptimizations(packageName)) "kaldırıldı" else "VAR").append('\n')
            append("Yansıtma: ").append(if (MirrorBridge.isProjecting) "ÇALIŞIYOR" else "durdu").append('\n')
            append("Araç ekranı: ").append(if (MirrorBridge.hasCarSurface) "bağlı" else "bağlı değil")
        }
    }

    private fun requestProjection() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIF)
            return
        }
        val mpm = getSystemService(MediaProjectionManager::class.java)
        @Suppress("DEPRECATION")
        startActivityForResult(mpm.createScreenCaptureIntent(), REQ_PROJECTION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_NOTIF) requestProjection()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_PROJECTION || resultCode != RESULT_OK || data == null) return
        startForegroundService(
            Intent(this, MirrorService::class.java)
                .putExtra(MirrorService.EXTRA_CODE, resultCode)
                .putExtra(MirrorService.EXTRA_DATA, data)
        )
        status.postDelayed({ refresh() }, 500)
    }

    companion object {
        private const val REQ_PROJECTION = 1
        private const val REQ_NOTIF = 2
    }
}

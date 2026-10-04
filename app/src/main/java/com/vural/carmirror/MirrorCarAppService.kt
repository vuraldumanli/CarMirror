package com.vural.carmirror

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import androidx.car.app.AppManager
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.validation.HostValidator
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class MirrorCarAppService : CarAppService() {
    // Kişisel, Play Store dışı kullanım için tüm hostlara izin veriliyor
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = object : Session() {
        override fun onCreateScreen(intent: Intent): Screen = MirrorScreen(carContext)
    }
}

class MirrorScreen(carContext: CarContext) : Screen(carContext), SurfaceCallback {

    private val gestures = GestureForwarder(carContext)

    init {
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(this)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                if (!MirrorBridge.isProjecting) {
                    CarToast.makeText(
                        carContext,
                        "Telefonda 'Ekran Yansıt' uygulamasını açıp Başlat'a basın",
                        CarToast.LENGTH_LONG
                    ).show()
                    Notifications.askToStart(carContext)
                } else if (!TouchService.isEnabled) {
                    CarToast.makeText(
                        carContext,
                        "Dokunma için erişilebilirlik izni kapalı",
                        CarToast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                gestures.cancel()
                MirrorBridge.detachSurface(null)
            }
        })
    }

    override fun onGetTemplate(): Template {
        fun act(title: String, global: Int) = Action.Builder()
            .setTitle(title)
            .setOnClickListener { TouchService.global(global) }
            .build()

        val strip = ActionStrip.Builder()
            .addAction(act("Geri", AccessibilityService.GLOBAL_ACTION_BACK))
            .addAction(act("Ana", AccessibilityService.GLOBAL_ACTION_HOME))
            .addAction(act("Son", AccessibilityService.GLOBAL_ACTION_RECENTS))
            .build()

        val mapStrip = ActionStrip.Builder()
            .addAction(Action.PAN)
            .build()

        return NavigationTemplate.Builder()
            .setActionStrip(strip)
            .setMapActionStrip(mapStrip)
            .build()
    }

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        val s = surfaceContainer.surface ?: return
        MirrorBridge.attachSurface(s, surfaceContainer.width, surfaceContainer.height, surfaceContainer.dpi)
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        MirrorBridge.detachSurface(surfaceContainer.surface)
    }

    override fun onClick(x: Float, y: Float) = gestures.click(x, y)

    override fun onScroll(distanceX: Float, distanceY: Float) = gestures.scroll(distanceX, distanceY)

    override fun onFling(velocityX: Float, velocityY: Float) = gestures.fling(velocityX, velocityY)

    override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) =
        gestures.scale(focusX, focusY, scaleFactor)
}

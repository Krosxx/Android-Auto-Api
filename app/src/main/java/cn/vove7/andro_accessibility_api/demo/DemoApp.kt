package cn.vove7.andro_accessibility_api.demo

import android.app.Application
import android.content.Intent
import android.os.Build
import cn.vove7.andro_accessibility_api.AccessibilityApi
import cn.vove7.andro_accessibility_api.demo.service.AppAccessibilityService
import cn.vove7.andro_accessibility_api.demo.service.ForegroundService
import timber.log.Timber

/**
 * # DemoApp
 *
 *
 * Created on 2020/6/10
 *
 * @author Vove
 */
class DemoApp : Application() {
    companion object {
        lateinit var INS: Application
    }

    override fun onCreate() {
        INS = this
        super.onCreate()

        if (Timber.treeCount == 0) {
            Timber.plant(Timber.DebugTree())
        }

        AccessibilityApi.init(this,
            AppAccessibilityService::class.java
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, ForegroundService::class.java))
        } else {
            startService(Intent(this, ForegroundService::class.java))
        }
        Timber.i("DemoApp create.")
    }
}
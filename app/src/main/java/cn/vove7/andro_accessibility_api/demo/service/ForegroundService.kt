package cn.vove7.andro_accessibility_api.demo.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import cn.vove7.auto.core.api.back
import cn.vove7.auto.core.api.printLayoutInfo
import cn.vove7.andro_accessibility_api.demo.R
import cn.vove7.andro_accessibility_api.demo.launchWithExpHandler
import kotlinx.coroutines.delay

/**
 * # ForegroundService
 *
 * Created on 2020/6/11
 * @author Vove
 */
class ForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    private val channelId by lazy {
        val id = "ForegroundService"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val c = NotificationChannel(
                id,
                getString(R.string.fore_service),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(c)
        }
        id
    }

    private fun getNotification() = NotificationCompat.Builder(this, channelId).apply {
        setContentTitle(getString(R.string.fore_service))
        setContentText("输出布局 on logcat")
        val printIntent = Intent(this@ForegroundService, ForegroundService::class.java)
        printIntent.action = ACTION_PRINT_LAYOUT
        val pi = PendingIntent.getService(this@ForegroundService, 0, printIntent, PendingIntent.FLAG_MUTABLE)

        setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        setSmallIcon(R.mipmap.ic_launcher_round)
        val acb = NotificationCompat.Action.Builder(0, "输出布局 on logcat", pi)
        addAction(acb.build())
        setOngoing(true)
    }.build()

    override fun onCreate() {
        super.onCreate()
        startForeground(1999, getNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.action?.also {
            parseAction(it)
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun parseAction(action: String) {
        when (action) {
            ACTION_PRINT_LAYOUT -> {
                launchWithExpHandler {
                    back()
                    delay(1000)
                    printLayoutInfo()
                }
            }
        }

    }

    companion object {
        const val ACTION_PRINT_LAYOUT = "print_layout"
    }
}
package cn.vove7.andro_accessibility_api.api

import android.os.Build
import androidx.annotation.RequiresApi
import cn.vove7.andro_accessibility_api.AccessibilityApi
import cn.vove7.andro_accessibility_api.AppScope
import cn.vove7.andro_accessibility_api.utils.whileWaitTime
import kotlinx.coroutines.delay
import java.lang.Thread.sleep
import kotlin.math.min

/**
 * # nav_api
 *
 * Created on 2020/6/10
 * @author Vove
 */

fun back(): Boolean = AccessibilityApi.requireBase.back()

fun home(): Boolean = AccessibilityApi.requireBase.home()

fun powerDialog(): Boolean = AccessibilityApi.requireBase.powerDialog()

fun pullNotificationBar(): Boolean =
    AccessibilityApi.requireBase.notificationBar()

fun quickSettings(): Boolean = AccessibilityApi.requireBase.quickSettings()

fun recents(): Boolean = AccessibilityApi.requireBase.recents()

@RequiresApi(Build.VERSION_CODES.P)
fun lockScreen(): Boolean = AccessibilityApi.requireBase.lockScreen()

@RequiresApi(Build.VERSION_CODES.P)
fun screenShot(): Boolean = AccessibilityApi.requireBase.screenShot()

@RequiresApi(Build.VERSION_CODES.N)
fun splitScreen(): Boolean = AccessibilityApi.requireBase.splitScreen()

suspend fun waitForApp(pkg: String, waitTime: Long = 30000): Boolean {
    return waitForPage(AppScope(pkg, ""), waitTime)
}

suspend fun waitForPage(scope: AppScope, waitTime: Long = 30000): Boolean {
    requireBaseAccessibility()
    return whileWaitTime(min(waitTime, 30000), 100) {
        if (AccessibilityApi.currentScope == scope) true
        else null
    } ?: false
}

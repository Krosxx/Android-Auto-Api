package cn.vove7.andro_accessibility_api.api

import android.os.Build
import androidx.annotation.RequiresApi
import cn.vove7.andro_accessibility_api.AccessibilityApi
import cn.vove7.andro_accessibility_api.AppScope
import cn.vove7.andro_accessibility_api.utils.whileWaitTime
import java.lang.Thread.sleep
import kotlin.math.min

/**
 * # nav_api
 *
 * Created on 2020/6/10
 * @author Vove
 */

fun back(): Boolean = AccessibilityApi.baseService?.back() ?: false

fun home(): Boolean = AccessibilityApi.baseService?.home() ?: false

fun powerDialog(): Boolean = AccessibilityApi.baseService?.powerDialog() ?: false

fun pullNotificationBar(): Boolean =
    AccessibilityApi.baseService?.notificationBar() ?: false

fun quickSettings(): Boolean = AccessibilityApi.baseService?.quickSettings() ?: false

fun recents(): Boolean = AccessibilityApi.baseService?.recents() ?: false

@RequiresApi(Build.VERSION_CODES.P)
fun lockScreen(): Boolean = AccessibilityApi.baseService?.lockScreen() ?: false

@RequiresApi(Build.VERSION_CODES.P)
fun screenShot(): Boolean = AccessibilityApi.baseService?.screenShot() ?: false

@RequiresApi(Build.VERSION_CODES.N)
fun splitScreen(): Boolean = AccessibilityApi.baseService?.splitScreen() ?: false

fun waitForApp(pkg: String, waitTime: Long = 30000): Boolean {
    if (!AccessibilityApi.isBaseServiceEnable) {
        return false
    }
    return waitForPage(AppScope(pkg, ""), waitTime)
}

fun waitForPage(scope: AppScope, waitTime: Long = 30000): Boolean {
    return whileWaitTime(min(waitTime, 30000)) {
        if (AccessibilityApi.currentScope == scope) true else {
            sleep(100)
            null
        }
    } ?: false
}
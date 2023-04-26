package cn.vove7.auto.core.api

import android.os.Build
import androidx.annotation.RequiresApi
import cn.vove7.auto.core.AppScope
import cn.vove7.auto.core.AutoApi
import cn.vove7.auto.core.utils.whileWaitTime
import kotlin.math.min

/**
 * # nav_api
 *
 * Created on 2020/6/10
 * @author Vove
 */

fun back(): Boolean = AutoApi.back()

fun home(): Boolean = AutoApi.home()

fun powerDialog(): Boolean = AutoApi.powerDialog()

fun pullNotificationBar(): Boolean =
    AutoApi.notificationBar()

fun quickSettings(): Boolean = AutoApi.quickSettings()

fun recents(): Boolean = AutoApi.recents()

@RequiresApi(Build.VERSION_CODES.P)
fun lockScreen(): Boolean = AutoApi.lockScreen()

@RequiresApi(Build.VERSION_CODES.P)
fun screenShot(): Boolean = AutoApi.screenShot()

@RequiresApi(Build.VERSION_CODES.N)
fun splitScreen(): Boolean = AutoApi.splitScreen()

suspend fun waitForApp(pkg: String, waitTime: Long = 30000): Boolean {
    return waitForPage(AppScope(pkg, ""), waitTime)
}

suspend fun waitForPage(scope: AppScope, waitTime: Long = 30000): Boolean {
    return whileWaitTime(min(waitTime, 30000), 100) {
        if (AutoApi.currentScope == scope) true
        else null
    } ?: false
}

package cn.vove7.andro_accessibility_api.api

import android.accessibilityservice.AccessibilityService
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * # BaseServiceApi
 *
 * Created on 2020/6/10
 * @author Vove
 */
interface BaseServiceApi {
    val _baseService: AccessibilityService

    //返回操作
    fun back(): Boolean = performAction(AccessibilityService.GLOBAL_ACTION_BACK)

    //返回桌面
    fun home(): Boolean = performAction(AccessibilityService.GLOBAL_ACTION_HOME)

    //电源菜单
    fun powerDialog(): Boolean =
        performAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)

    //通知栏
    fun notificationBar(): Boolean =
        performAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)

    //展开通知栏 > 快捷设置
    fun quickSettings(): Boolean =
        performAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)

    //锁屏
    @RequiresApi(Build.VERSION_CODES.P)
    fun lockScreen(): Boolean = performAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)

    //截屏
    @RequiresApi(Build.VERSION_CODES.P)
    fun screenShot(): Boolean =
        performAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)

    //最近任务
    fun recents(): Boolean = performAction(AccessibilityService.GLOBAL_ACTION_RECENTS)

    //分屏
    @RequiresApi(api = Build.VERSION_CODES.N)
    fun splitScreen(): Boolean =
        performAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)

    private fun performAction(action: Int): Boolean {
        return _baseService.performGlobalAction(action)
    }

}
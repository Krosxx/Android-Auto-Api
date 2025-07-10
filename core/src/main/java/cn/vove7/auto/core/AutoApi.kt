package cn.vove7.auto.core

import android.accessibilityservice.AccessibilityService
import android.app.Instrumentation
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.SparseArray
import android.view.Display
import android.view.InputEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.annotation.RequiresApi
import cn.vove7.auto.core.utils.AutoGestureDescription
import cn.vove7.auto.core.utils.AutoServiceUnavailableException
import cn.vove7.auto.core.utils.GestureResultCallback
import cn.vove7.auto.core.utils.ensureNotInMainThread
import cn.vove7.auto.core.utils.getApplication
import java.lang.reflect.Proxy
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.jvm.Throws

/**
 * # AutoApi
 *
 * Created on 2020/6/10
 * @author Vove
 */

fun requireAutoService() = requireImpl()

fun requireImpl(): AutoApi {
    return AutoApi.AutoImpl?.also {
        if (!it.isEnabled()) { // check
            AutoApi.clearImpl()
            throw AutoServiceUnavailableException()
        }
    } ?: throw AutoServiceUnavailableException()
}

fun buildProxy(): AutoApi = Proxy.newProxyInstance(
    getApplication().classLoader, arrayOf(AutoApi::class.java)
) { _, method, args ->
    if (args == null) method?.invoke(requireImpl())
    else method?.invoke(requireImpl(), *args)
} as AutoApi


interface AutoApi {

    @Suppress("MemberVisibilityCanBePrivate", "unused")
    companion object : AutoApi by buildProxy() {
        internal var AutoImpl: AutoApi? = null

        val appCtx: Context by lazy {
            getApplication()
        }

        val serviceType: Int
            get() = when (AutoImpl) {
                is AccessibilityService -> SERVICE_TYPE_ACCESSIBILITY
                is Instrumentation -> SERVICE_TYPE_INSTRUMENTATION
                else -> SERVICE_TYPE_NONE
            }

        fun isServiceEnabled(): Boolean = AutoImpl?.isEnabled() ?: false

        fun setImpl(impl: AutoApi) {
            AutoImpl = impl
        }

        fun clearImpl() {
            AutoImpl = null
        }

        const val SERVICE_TYPE_NONE = 0
        const val SERVICE_TYPE_ACCESSIBILITY = 1
        const val SERVICE_TYPE_INSTRUMENTATION = 2

    }

    fun isEnabled() = AutoImpl != null

    fun rootInActiveWindow(): AccessibilityNodeInfo?

    fun windows(): List<AccessibilityWindowInfo>?

    @RequiresApi(Build.VERSION_CODES.R)
    fun windowsOnAllDisplays(): SparseArray<List<AccessibilityWindowInfo>>

    // 返回操作
    fun back(): Boolean = performAction(AccessibilityService.GLOBAL_ACTION_BACK)

    // 返回桌面
    fun home(): Boolean = performAction(AccessibilityService.GLOBAL_ACTION_HOME)

    // 电源菜单
    fun powerDialog(): Boolean =
        performAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)

    // 通知栏
    fun notificationBar(): Boolean =
        performAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)

    // 展开通知栏 > 快捷设置
    fun quickSettings(): Boolean =
        performAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)

    // 锁屏
    @RequiresApi(Build.VERSION_CODES.P)
    fun lockScreen(): Boolean = performAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)

    // 截屏
    @RequiresApi(Build.VERSION_CODES.P)
    fun screenShot(): Boolean =
        performAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)

    val currentScope: AppScope?
        get() = requireImpl().let {
            PageUpdateMonitor.currentScope
        }

    // activity or dialog
    val currentPage: String? get() = currentScope?.pageName

    // 最近任务
    fun recents(): Boolean = performAction(AccessibilityService.GLOBAL_ACTION_RECENTS)

    // 分屏
    @RequiresApi(api = Build.VERSION_CODES.N)
    fun splitScreen(): Boolean =
        performAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)

    fun performAction(action: Int): Boolean

    // 同步执行手势 (不可于主线程调用)
    // 返回执行是否成功
    suspend fun doGestureSync(
        gesture: AutoGestureDescription
    ): Boolean {
        ensureNotInMainThread("doGestureSync")
        return suspendCoroutine { cont ->
            doGestureAsync(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: AutoGestureDescription?) {
                    cont.resume(true)
                }

                override fun onCancelled(gestureDescription: AutoGestureDescription?) {
                    cont.resume(false)
                }
            }, Handler(Looper.getMainLooper()))
        }
    }

    // 异步执行手势
    fun doGestureAsync(
        gesture: AutoGestureDescription,
        callback: GestureResultCallback?,
        handler: Handler?
    )

    fun injectInputEvent(event: InputEvent, sync: Boolean) {
        throw NotImplementedError("not support")
    }

    fun sendString(text: String) {
        throw NotImplementedError("not support")
    }

    fun sendKeyCode(keyCode: Int): Boolean {
        throw NotImplementedError("not support")
    }

    // Accessibility 方式需要 Android 11+
    // Instrumentation 不支持指定 displayId
    @Throws(TimeoutException::class, RuntimeException::class)
    suspend fun takeScreenshot(displayId: Int = Display.DEFAULT_DISPLAY): Bitmap?

    fun registerImpl() {
        setImpl(this)
    }

    fun destroyAutoService() {
        clearImpl()
    }
}
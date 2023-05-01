@file:Suppress("unused")

package cn.vove7.andro_accessibility_api

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.util.SparseArray
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.annotation.RequiresApi
import cn.vove7.auto.core.AppScope
import cn.vove7.auto.core.AutoApi
import cn.vove7.auto.core.OnPageUpdate
import cn.vove7.auto.core.PageUpdateMonitor
import cn.vove7.auto.core.utils.AutoGestureDescription
import cn.vove7.auto.core.utils.convert
import cn.vove7.auto.core.utils.jumpAccessibilityServiceSettings
import cn.vove7.auto.core.utils.whileWaitTime
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.min
import cn.vove7.auto.core.utils.GestureResultCallback as GestureCallback

/**
 *
 *
 * Created by Vove on 2018/6/18
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class AccessibilityApi : AccessibilityService(), AutoApi {

    abstract val enableListenPageUpdate: Boolean

    override fun performAction(action: Int) = this.performGlobalAction(action)
    override fun rootInActiveWindow() = rootInActiveWindow

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun windows(): List<AccessibilityWindowInfo>? = windows

    private val pageListener: OnPageUpdate = ::onPageUpdate

    override fun onServiceConnected() {
        if (this::class.java == BASE_SERVICE_CLS) {
            baseService = this
        }
        if (isEnableGestureService() && this::class.java == GESTURE_SERVICE_CLS) {
            gestureService = this
        }
        registerImpl()
        PageUpdateMonitor.enableListenPageUpdate = enableListenPageUpdate
        PageUpdateMonitor.addOnPageUpdateListener(pageListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        AutoApi.clearImpl()
        PageUpdateMonitor.removeOnPageUpdateListener(pageListener)
        if (this::class.java == BASE_SERVICE_CLS) {
            baseService = null
        }
        if (isEnableGestureService() && this::class.java == GESTURE_SERVICE_CLS) {
            gestureService = null
        }
    }

    override fun getRootInActiveWindow(): AccessibilityNodeInfo? {
        return try {
            super.getRootInActiveWindow()
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Activity or Dialog update
     * @param currentScope AppScope
     */
    open fun onPageUpdate(currentScope: AppScope) {}

    /**
     * @param event AccessibilityEvent?
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!enableListenPageUpdate || event == null) return
        PageUpdateMonitor.onAccessibilityEvent(event)
    }

    override fun takeScreenshot(): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return runBlocking {
                suspendCoroutine<Bitmap> { cont ->
                    super.takeScreenshot(0, appCtx.mainExecutor, object : TakeScreenshotCallback {
                        override fun onSuccess(screenshot: ScreenshotResult) {
                            val bitmap = Bitmap.wrapHardwareBuffer(screenshot.hardwareBuffer, screenshot.colorSpace)
                            if (bitmap != null) {
                                cont.resume(bitmap)
                            }
                        }

                        override fun onFailure(errorCode: Int) {
                            cont.resumeWithException(RuntimeException("takeScreenshot failed, code: $errorCode"))
                        }
                    })
                }
            }
        }
        return null
    }

    override fun onInterrupt() {
    }

    companion object {
        lateinit var BASE_SERVICE_CLS: Class<*>
        lateinit var GESTURE_SERVICE_CLS: Class<*>

        @SuppressLint("StaticFieldLeak")
        private var appCtx_: Context? = null
        val appCtx
            get() = appCtx_ ?: throw NullPointerException(
                "please call AccessibilityApi.init(...) in Application.onCreate()")

        fun init(
            ctx: Context,
            baseServiceCls: Class<*>,
            gestureServiceCls: Class<*> = baseServiceCls
        ) {
            appCtx_ = ctx.applicationContext
            BASE_SERVICE_CLS = baseServiceCls
            GESTURE_SERVICE_CLS = gestureServiceCls
        }

        private fun isEnableGestureService() = ::GESTURE_SERVICE_CLS.isInitialized

        // 无障碍基础服务
        @SuppressLint("StaticFieldLeak")
        var baseService: AccessibilityApi? = null

        val requireBase: AccessibilityApi
            get() = run {
                requireBaseAccessibility(false)
                baseService!!
            }

        // 无障碍高级服务 执行手势等操作
        /**
         * GestureService base on AccessibilityApi
         */
        @SuppressLint("StaticFieldLeak")
        var gestureService: AccessibilityService? = null

        val requireGesture: AccessibilityService
            get() = run {
                requireGestureAccessibility(false)
                gestureService!!
            }

        // currentAppScope
        val currentScope get() = AutoApi.currentScope

        // Service is enable
        val isBaseServiceEnable: Boolean
            get() = (baseService != null)

        val isServiceEnable: Boolean
            get() = isBaseServiceEnable

        val isGestureServiceEnable: Boolean get() = gestureService != null

        /**
         * 等待无障碍开启，最长等待30s
         * @param waitMillis Long
         * @return Boolean true 开启成功 ; false 超时
         * @throws NeedAccessibilityException
         */
        @JvmOverloads
        @JvmStatic
        @Throws(NeedAccessibilityException::class)
        suspend fun waitAccessibility(waitMillis: Long = 30000, cls: Class<*>): Boolean {

            val se = if (cls == BASE_SERVICE_CLS) isBaseServiceEnable
            else isGestureServiceEnable

            if (se) return true
            else jumpAccessibilityServiceSettings(cls)

            return whileWaitTime(min(30000, waitMillis), 500) {
                if (isBaseServiceEnable) true
                else null
            } ?: throw NeedAccessibilityException(cls.name)
        }

        // 声明 需要基础无障碍权限
        fun requireBaseAccessibility(autoJump: Boolean = false) {
            if (!isBaseServiceEnable) {
                if (autoJump) jumpAccessibilityServiceSettings(BASE_SERVICE_CLS)
                throw NeedAccessibilityException(BASE_SERVICE_CLS.name)
            }
        }

        // 声明 需要手势无障碍权限
        fun requireGestureAccessibility(autoJump: Boolean = false) {
            if (!isGestureServiceEnable) {
                if (autoJump) jumpAccessibilityServiceSettings(GESTURE_SERVICE_CLS)
                throw NeedAccessibilityException(GESTURE_SERVICE_CLS.name)
            }
        }

    }

    override suspend fun doGesturesAsync(gesture: AutoGestureDescription, callback: GestureCallback?, handler: Handler?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            throw IllegalStateException("dispatchGesture require android N+")
        }
        requireGesture.dispatchGesture(gesture.convert(), callback?.let { cb ->
            @RequiresApi(Build.VERSION_CODES.N)
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    cb.onCompleted(gesture)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    cb.onCancelled(gesture)
                }
            }
        }, handler)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun windowsOnAllDisplays(): SparseArray<List<AccessibilityWindowInfo>> {
        return requireBase.windowsOnAllDisplays
    }
}


fun requireBaseAccessibility(autoJump: Boolean = false) {
    AccessibilityApi.requireBaseAccessibility(autoJump)
}

suspend fun waitBaseAccessibility(waitMillis: Long = 30000) {
    AccessibilityApi.waitAccessibility(waitMillis, AccessibilityApi.BASE_SERVICE_CLS)
}

fun requireGestureAccessibility(autoJump: Boolean = false) {
    AccessibilityApi.requireGestureAccessibility(autoJump)
}

suspend fun waitGestureAccessibility(waitMillis: Long = 30000) {
    AccessibilityApi.waitAccessibility(waitMillis, AccessibilityApi.GESTURE_SERVICE_CLS)
}

suspend fun waitAccessibility(waitMillis: Long = 30000, cls: Class<*>): Boolean {
    return AccessibilityApi.waitAccessibility(waitMillis, cls)
}


/**
 * 无障碍服务未运行异常
 * @constructor
 */
open class NeedAccessibilityException(name: String?) : RuntimeException("无障碍服务未运行: $name")

class NeedBaseAccessibilityException :
    NeedAccessibilityException(AccessibilityApi.BASE_SERVICE_CLS.name)

class NeedGestureAccessibilityException :
    NeedAccessibilityException(AccessibilityApi.GESTURE_SERVICE_CLS.name)


package cn.vove7.accessibility.uiauto

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Instrumentation
import android.app.UiAutomation
import android.content.Context
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.os.*
import android.util.SparseArray
import android.view.*
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOWS_CHANGED
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.annotation.CallSuper
import androidx.annotation.RequiresApi
import cn.vove7.auto.core.AppScope
import cn.vove7.auto.core.AutoApi
import cn.vove7.auto.core.OnPageUpdate
import cn.vove7.auto.core.PageUpdateMonitor
import cn.vove7.auto.core.utils.AutoGestureDescription
import cn.vove7.auto.core.utils.GestureResultCallback
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

/**
 * # AutoInstrumentation
 *
 * @author Vove
 * @date 2023/4/25
 */
@Suppress("unused")
open class AutoInstrumentation : Instrumentation(), AutoApi {

    open val enableListenPageUpdate: Boolean = true
    private val pageListener: OnPageUpdate = ::onPageUpdate

    companion object {
        var INS: Instrumentation? = null
        val uiAutomation: UiAutomation? get() = INS?.uiAutomation
    }

    private var gestureSeq = AtomicInteger(0)
    private val motionEventInjector by lazy {
        MotionInjector(Looper.getMainLooper(), uiAutomation)
    }

    override fun onCreate(arguments: Bundle?) {
        if (Timber.treeCount == 0) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.i("AutoInstrumentation onCreate.")
        INS = this
        start()
    }

    private fun initPageListener() {
        PageUpdateMonitor.enableListenPageUpdate = enableListenPageUpdate
        PageUpdateMonitor.addOnPageUpdateListener(pageListener)
        uiAutomation.setOnAccessibilityEventListener(PageUpdateMonitor)
    }

    private fun initServiceInfo() {
        val si = uiAutomation.serviceInfo
        si.eventTypes = TYPE_WINDOWS_CHANGED
        si.flags = si.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
            AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY
        si.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        si.packageNames = null
        onBuildServiceInfo(si)
        Timber.i("uiAutomation.serviceInfo: $si")
        uiAutomation.serviceInfo = si
    }

    open fun onBuildServiceInfo(serviceInfo: AccessibilityServiceInfo) {
    }

    @CallSuper
    override fun onStart() {
        initServiceInfo()
        registerImpl()
        initPageListener()

        Timber.i("AutoInstrumentation started.")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Timber.i("windowsOnAllDisplays: ${uiAutomation.windowsOnAllDisplays}")
        }
        Timber.i("windows: ${uiAutomation.windows}")

    }

    open fun onPageUpdate(currentScope: AppScope) {}

    override fun isEnabled(): Boolean {
        kotlin.runCatching {
            if(!uiAutomation.injectInputEvent(MotionEvent.obtain(0, 0, 0, 0f, 0f, 0), false)){
                return false
            }
        }.onFailure {
            Timber.w(it)
            return false
        }
        return super.isEnabled()
    }

    override fun rootInActiveWindow(): AccessibilityNodeInfo? = uiAutomation.rootInActiveWindow

    override fun windows(): List<AccessibilityWindowInfo>? = uiAutomation.windows

    @RequiresApi(Build.VERSION_CODES.R)
    override fun windowsOnAllDisplays(): SparseArray<List<AccessibilityWindowInfo>> {
        return uiAutomation.windowsOnAllDisplays
    }

    override fun performAction(action: Int) = uiAutomation.performGlobalAction(action)

    override suspend fun doGesturesAsync(
        gesture: AutoGestureDescription,
        callback: GestureResultCallback?,
        handler: Handler?
    ) {
        val displayId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) gesture.displayId else 0
        val sampleTimeMs = calculateGestureSampleTimeMs(displayId)
        val steps = AutoGestureDescription.MotionEventGenerator.getGestureStepsFromGestureDescription(gesture, sampleTimeMs)
        val seq = gestureSeq.getAndAdd(1)
        motionEventInjector.injectEvents(steps, seq, displayId) { _, success ->
            if (success) {
                callback?.onCompleted(gesture)
            } else {
                callback?.onCancelled(gesture)
            }
        }
    }

    override fun injectInputEvent(event: InputEvent, sync: Boolean) {
        uiAutomation.injectInputEvent(event, sync)
    }

    override fun sendString(text: String) = sendStringSync(text)


    override fun sendKeyCode(keyCode: Int): Boolean {
        val eventTime = SystemClock.uptimeMillis()
        val downEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN,
            keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
            InputDevice.SOURCE_KEYBOARD)
        if (uiAutomation.injectInputEvent(downEvent, false)) {
            val upEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP,
                keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
                InputDevice.SOURCE_KEYBOARD)
            if (uiAutomation.injectInputEvent(upEvent, false)) {
                return true
            }
        }
        return false
    }

    override fun takeScreenshot(): Bitmap? {
        return uiAutomation.takeScreenshot()
    }

    private fun calculateGestureSampleTimeMs(displayId: Int): Int {
        val display: Display = (context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).getDisplay(
            displayId) ?: return 100
        val msPerSecond = 1000
        val sampleTimeMs = (msPerSecond / display.refreshRate).toInt()
        return if (sampleTimeMs < 1) {
            // Should be impossible, but do not return 0.
            100
        } else sampleTimeMs
    }

    override fun onDestroy() {
        Timber.i("AutoInstrumentation destroyed.")
        motionEventInjector.onDestroy()
        INS = null
        PageUpdateMonitor.removeOnPageUpdateListener(pageListener)
        destroyAutoService()
    }

}

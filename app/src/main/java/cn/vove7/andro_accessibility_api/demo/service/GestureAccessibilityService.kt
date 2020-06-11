package cn.vove7.andro_accessibility_api.demo.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import cn.vove7.andro_accessibility_api.AccessibilityApi

/**
 * # GestureAccessibilityService
 *
 * Created on 2020/6/10
 * @author Vove
 */
class GestureAccessibilityService : AccessibilityService() {
    override fun onCreate() {
        super.onCreate()
        //must
        AccessibilityApi.gestureService = this
    }

    override fun onDestroy() {
        super.onDestroy()
        //must
        AccessibilityApi.gestureService = null
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
}
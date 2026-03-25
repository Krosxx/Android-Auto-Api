package cn.vove7.auto.core

import android.app.UiAutomation
import android.view.View
import android.view.accessibility.AccessibilityEvent
import cn.vove7.auto.core.PageUpdateMonitor.currentAppPageInfo
import cn.vove7.auto.core.viewnode.ViewNode
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Collections

/**
 * # PageUpdateMonitor
 *
 * @author Vove
 * @date 2023/4/25
 */

typealias OnPageUpdate = (score: AppPageInfo) -> Unit

object PageUpdateMonitor : UiAutomation.OnAccessibilityEventListener {

    var enableListenPageUpdate: Boolean = true

    internal var currentAppPageInfo: AppPageInfo? = null

    private val onPageUpdateListeners = Collections.synchronizedList<OnPageUpdate>(arrayListOf())

    fun addOnPageUpdateListener(listener: OnPageUpdate) {
        onPageUpdateListeners.add(listener)
    }

    fun removeOnPageUpdateListener(listener: OnPageUpdate) {
        onPageUpdateListeners.remove(listener)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!enableListenPageUpdate) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // 界面切换
            val classNameStr = event.className
            val pkg = event.packageName as String?
            if (!classNameStr.isNullOrBlank() && pkg != null) {
                GlobalScope.launch {
                    updateCurrentApp(pkg, classNameStr.toString())
                }
            }
        }
    }


    /**
     * 更新当前[currentAppPageInfo]
     * @param pkg String
     * @param pageName String Activity or Dialog
     */
    private fun updateCurrentApp(pkg: String, pageName: String) {
        val actPkg = ViewNode.activeWinNode()?.packageName
        if (actPkg != null && actPkg != pkg) {
            // 忽略非 active 窗口
            return
        }
        if (currentAppPageInfo?.packageName == pkg && pageName == currentAppPageInfo?.pageName) {
            return
        }
        if (
            pageName.startsWith("android.widget") ||
            pageName.startsWith("android.view") ||
            pageName.startsWith("android.inputmethodservice") ||
            pageIsView(pageName)
        ) {
            return
        }
        AppPageInfo(pkg, pageName).also {
            currentAppPageInfo = it
            onPageUpdate(it)
        }
    }

    private fun onPageUpdate(currentScope: AppPageInfo) {
        onPageUpdateListeners.forEach { it.invoke(currentScope) }
    }

    private fun pageIsView(pageName: String): Boolean = try {
        View::class.java.isAssignableFrom(Class.forName(pageName))
    } catch (e: ClassNotFoundException) {
        false
    }

}
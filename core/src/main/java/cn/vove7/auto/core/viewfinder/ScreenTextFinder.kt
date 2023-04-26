package cn.vove7.auto.core.viewfinder

import android.view.accessibility.AccessibilityNodeInfo
import cn.vove7.auto.core.viewnode.ViewNode

/**
 * # ScreenTextFinder
 *
 * @author Vove
 * 2018/10/14
 */
class ScreenTextFinder(
    node: ViewNode? = null
) : ViewFinder<ScreenTextFinder>(node) {
    override fun finderInfo() = "ScreenTextFinder"

    var isWeb = false

    override fun findCondition(node: AccessibilityNodeInfo): Boolean {
        if (node.className?.endsWith("WebView", ignoreCase = true) == true) {
            isWeb = true
            return false
        }
        return ((node.childCount == 0) && (node.text != null && node.text.trim() != "")
            || (isWeb && (node.contentDescription ?: "") != ""))
    }

}
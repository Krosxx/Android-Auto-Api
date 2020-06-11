package cn.vove7.andro_accessibility_api.viewfinder

import android.view.accessibility.AccessibilityNodeInfo
import cn.vove7.andro_accessibility_api.viewnode.ViewNode

/**
 * # CustomViewFinder
 *
 * Created on 2020/6/11
 * @author Vove
 */
class CustomViewFinder(
    node: ViewNode? = null,
    val predicate: (AccessibilityNodeInfo) -> Boolean
) : ViewFinder(node) {
    override fun findCondition(node: AccessibilityNodeInfo): Boolean = predicate(node)
}
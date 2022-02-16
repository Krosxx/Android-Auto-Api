package cn.vove7.andro_accessibility_api.viewfinder

import android.view.accessibility.AccessibilityNodeInfo
import cn.vove7.andro_accessibility_api.viewnode.ViewNode
import kotlin.coroutines.CoroutineContext

/**
 * # CustomViewFinder
 *
 * Created on 2020/6/11
 * @author Vove
 */
class CustomViewFinder(
    override val node: ViewNode? = null,
    val predicate: (AccessibilityNodeInfo) -> Boolean
) : ViewFinder<CustomViewFinder> {
    override var coroutineCtx: CoroutineContext? = null

    override fun findCondition(node: AccessibilityNodeInfo): Boolean = predicate(node)
}
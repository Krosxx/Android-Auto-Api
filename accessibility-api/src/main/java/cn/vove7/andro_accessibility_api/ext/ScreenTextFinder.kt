package cn.vove7.andro_accessibility_api.ext

import android.view.accessibility.AccessibilityNodeInfo
import cn.vove7.andro_accessibility_api.viewfinder.ViewFinder
import cn.vove7.andro_accessibility_api.viewnode.ViewNode
import kotlin.coroutines.CoroutineContext

/**
 * # ScreenTextFinder
 *
 * @author Vove
 * 2018/10/14
 */
class ScreenTextFinder(
    override val node: ViewNode? = null
) : ViewFinder<ScreenTextFinder> {

    var isWeb = false
    override var coroutineCtx: CoroutineContext? = null

    override fun findCondition(node: AccessibilityNodeInfo): Boolean {
        if (node.className?.endsWith("WebView", ignoreCase = true) == true) {
            isWeb = true
            return false
        }
        return node.childCount == 0 && (node.text != null && node.text.trim() != "")
                || (isWeb && node.contentDescription ?: "" != "")
    }

}
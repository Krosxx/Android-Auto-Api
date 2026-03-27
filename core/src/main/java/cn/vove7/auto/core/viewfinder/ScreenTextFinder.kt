package cn.vove7.auto.core.viewfinder

import cn.vove7.auto.core.viewnode.ViewNode
import java.util.concurrent.atomic.AtomicBoolean

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

    override fun findCondition(node: AcsNode, interrupt: AtomicBoolean): Boolean {
        if (node.className?.endsWith("WebView", ignoreCase = true) == true) {
            isWeb = true
            return false
        }
        return ((node.childCount == 0) && (!node.text.isNullOrBlank())
            || (isWeb && (!node.contentDescription.isNullOrBlank())))
    }

}
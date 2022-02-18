package cn.vove7.andro_accessibility_api.api

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import cn.vove7.andro_accessibility_api.AccessibilityApi
import cn.vove7.andro_accessibility_api.utils.times
import cn.vove7.andro_accessibility_api.viewfinder.*
import cn.vove7.andro_accessibility_api.viewnode.ViewNode

/**
 * # apis
 * 快捷api
 *
 * Created on 2020/6/10
 * @author Vove
 */


fun requireBaseAccessibility(autoJump: Boolean = false) {
    AccessibilityApi.requireBaseAccessibility(autoJump)
}

fun waitBaseAccessibility(waitMillis: Long = 30000) {
    AccessibilityApi.waitAccessibility(waitMillis, AccessibilityApi.BASE_SERVICE_CLS)
}

fun requireGestureAccessibility(autoJump: Boolean = false) {
    AccessibilityApi.requireGestureAccessibility(autoJump)
}

fun waitGestureAccessibility(waitMillis: Long = 30000) {
    AccessibilityApi.waitAccessibility(waitMillis, AccessibilityApi.GESTURE_SERVICE_CLS)
}

fun waitAccessibility(waitMillis: Long = 30000, cls: Class<*>): Boolean {
    return AccessibilityApi.waitAccessibility(waitMillis, cls)
}

/**
 * id 搜索
 * @param id String
 * @return ViewFindBuilder
 */
fun withId(id: String): ViewFindBuilder {
    return ViewFindBuilder().apply {
        id(id)
    }
}

/**
 * 文本全匹配
 * @param text Array<out String>
 * @return ViewFindBuilder
 */
fun withText(vararg text: String): ViewFindBuilder {
    return ViewFindBuilder().apply {
        equalsText(*text)
    }
}

/**
 * class 类型
 * eg:
 * 1. TextView
 * 2. android.widget.TextView
 *
 * @param types Array<out String>
 * @return ViewFindBuilder
 */
fun withType(vararg types: String): ViewFindBuilder {
    return ViewFindBuilder().apply {
        this.type(*types)
    }
}

/**
 * desc
 * @param desc Array<out String>
 * @return ViewFindBuilder
 */
fun withDesc(vararg desc: String): ViewFindBuilder {
    return ViewFindBuilder().apply {
        this.desc(*desc)
    }
}

fun editor(): ViewFindBuilder {
    return ViewFindBuilder().apply {
        this.editable(true)
    }
}

/**
 * 视图深度
 * @param depths Array<Int>
 * @return ViewFindBuilder
 */
fun withDepths(vararg depths: Int): ViewNode? {
    return ViewFinder.findByDepths(*depths)
}

/**
 * 文本包含
 * @param text String
 * @return ViewFindBuilder
 */
fun containsText(vararg text: String): ViewFindBuilder {
    return ViewFindBuilder().apply {
        containsText(*text)
    }
}


/**
 * 正则匹配
 * @param regs Array<out String>
 * @return ViewFindBuilder
 */
fun matchesText(vararg regs: String): ViewFindBuilder {
    return ViewFindBuilder().apply {
        matchesText(*regs)
    }
}

/**
 * 输出布局
 * @param out PrintStream
 */
fun printLayoutInfo(includeInvisible: Boolean = true) {
    requireBaseAccessibility()
    AccessibilityApi.baseService!!.rootNodeOfAllWindows.printWithChild(0, 0, includeInvisible)
}

private fun ViewNode.printWithChild(
    index: Int,
    dep: Int,
    includeInvisible: Boolean
) {
    if (!includeInvisible && !isVisibleToUser) {
        Log.w("ViewNode", "*" * dep + "[$index] " + "InVisible")
        return
    }
    if (isVisibleToUser) {
        Log.d("ViewNode", "*" * dep + "[$index] " + toString())
    } else {
        Log.w("ViewNode", "*" * dep + "[$index] " + toString())
    }
    children.forEachIndexed { i, it ->
        it.printWithChild(i, dep + 1, includeInvisible)
    }
}

fun findWith(
    includeInvisible: Boolean = false,
    predicate: (AccessibilityNodeInfo) -> Boolean
): ViewNode? {
    return SF.where(predicate).findFirst(includeInvisible)
}

fun findAllWith(
    includeInvisible: Boolean = false,
    predicate: (AccessibilityNodeInfo) -> Boolean
): Array<ViewNode> {
    return SF.where(predicate).findAll(includeInvisible)
}

fun ViewNode.findWith(
    includeInvisible: Boolean = false,
    predicate: (AccessibilityNodeInfo) -> Boolean
): ViewNode? {
    return SmartFinder(this).where(predicate).findFirst(includeInvisible)
}

fun ViewNode.findAllWith(
    includeInvisible: Boolean = false,
    predicate: (AccessibilityNodeInfo) -> Boolean
): Array<ViewNode> {
    return SmartFinder(this).where(predicate).findAll(includeInvisible)
}

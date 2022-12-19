@file:Suppress("unused")

package cn.vove7.andro_accessibility_api.api

import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
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
 * id 搜索
 * @param id String
 * @return ViewFindBuilder
 */
fun withId(id: String): ConditionGroup {
    return SF.id(id)
}

/**
 * 文本全匹配
 * @param text Array<out String>
 * @return ViewFindBuilder
 */
fun withText(vararg text: String): ConditionGroup {
    return SF.text(*text)
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
fun withType(vararg types: String): ConditionGroup {
    return SF.type(*types)
}

/**
 * desc
 * @param desc Array<out String>
 * @return ViewFindBuilder
 */
fun withDesc(vararg desc: String): ConditionGroup {
    return SF.desc(*desc)
}

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
fun editor(): ConditionGroup {
    return SF.editable(true)
}

/**
 * 视图深度
 * @param depths Array<Int>
 * @return ViewFindBuilder
 */
suspend fun withDepths(vararg depths: Int): ViewNode? {
    return ViewFinder.findByDepths(*depths)
}

/**
 * 文本包含
 * @param text String
 * @return ViewFindBuilder
 */
fun containsText(vararg text: String): ConditionGroup {
    return SF.containsText(*text)
}


/**
 * 正则匹配
 * @param reg Array<out String>
 * @return ViewFindBuilder
 */
fun matchesText(reg: String): ConditionGroup {
    return SF.matchText(reg)
}

/**
 * 输出布局
 */
fun printLayoutInfo(includeInvisible: Boolean = true) {
    AccessibilityApi.requireBase.rootNodeOfAllWindows.printWithChild(0, 0, includeInvisible)
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

suspend fun findWith(
    includeInvisible: Boolean = false,
    predicate: (AccessibilityNodeInfo) -> Boolean
): ViewNode? {
    return SF.where(predicate).findFirst(includeInvisible)
}

suspend fun findAllWith(
    includeInvisible: Boolean = false,
    predicate: (AccessibilityNodeInfo) -> Boolean
): Array<ViewNode> {
    return SF.where(predicate).findAll(includeInvisible)
}

suspend fun ViewNode.findWith(
    includeInvisible: Boolean = false,
    predicate: (AccessibilityNodeInfo) -> Boolean
): ViewNode? {
    return SmartFinder(this).where(predicate).findFirst(includeInvisible)
}

suspend fun ViewNode.findAllWith(
    includeInvisible: Boolean = false,
    predicate: (AccessibilityNodeInfo) -> Boolean
): Array<ViewNode> {
    return SmartFinder(this).where(predicate).findAll(includeInvisible)
}

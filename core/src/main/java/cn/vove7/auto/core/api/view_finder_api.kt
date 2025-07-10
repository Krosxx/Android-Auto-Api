@file:Suppress("unused")

package cn.vove7.auto.core.api

import android.os.Build
import androidx.annotation.RequiresApi
import cn.vove7.auto.core.utils.times
import cn.vove7.auto.core.viewfinder.AcsNode
import cn.vove7.auto.core.viewfinder.ConditionGroup
import cn.vove7.auto.core.viewfinder.SF
import cn.vove7.auto.core.viewfinder.SmartFinder
import cn.vove7.auto.core.viewfinder.ViewFinder
import cn.vove7.auto.core.viewfinder.containsText
import cn.vove7.auto.core.viewfinder.desc
import cn.vove7.auto.core.viewfinder.editable
import cn.vove7.auto.core.viewfinder.id
import cn.vove7.auto.core.viewfinder.matchText
import cn.vove7.auto.core.viewfinder.text
import cn.vove7.auto.core.viewfinder.type
import cn.vove7.auto.core.viewnode.ViewNode

/**
 * # apis
 * 快捷api
 *
 * Created on 2020/6/10
 * @author Vove
 */


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
    return ViewNode.findByDepths(*depths)
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
 * 输出布局 到 StringBuilder
 */
fun buildLayoutInfo(
    sb: StringBuilder = StringBuilder(),
    includeInvisible: Boolean = true
): String {
    ViewNode.getRoot().printWithChild(sb, includeInvisible)
    return sb.toString()
}

fun ViewNode.printWithChild(
    sb: StringBuilder,
    includeGone: Boolean = true,
    prefix: String = "",
    level: Int = 0,
) {
    val d = "%02d".format(level)
    if (level == 0) {
        sb.appendLine("00[0/0] $d " + toString())
    }
    val lastIndex = childCount - 1
    children.forEachIndexed { i, it ->
        val isLast = i == lastIndex
        val connector = if (isLast) " ┗ " else " ┣ "
        if (includeGone || it?.isVisibleToUser == true) {
            sb.appendLine("$d$prefix$connector[${i + 1}/${lastIndex + 1}] " + it.toString())
        }
        val newPrefix = prefix + if (isLast) "  " else " ┃ "
        it?.printWithChild(sb, includeGone, newPrefix, level + 1)
    }
}

suspend fun findWith(
    includeInvisible: Boolean = false,
    predicate: (AcsNode) -> Boolean
): ViewNode? {
    return SF.where(predicate).findFirst(includeInvisible)
}

suspend fun findAllWith(
    includeInvisible: Boolean = false,
    predicate: (AcsNode) -> Boolean
): Array<ViewNode> {
    return SF.where(predicate).findAll(includeInvisible)
}

suspend fun ViewNode.findWith(
    includeInvisible: Boolean = false,
    predicate: (AcsNode) -> Boolean
): ViewNode? {
    return SmartFinder(this).where(predicate).findFirst(includeInvisible)
}

suspend fun ViewNode.findAllWith(
    includeInvisible: Boolean = false,
    predicate: (AcsNode) -> Boolean
): Array<ViewNode> {
    return SmartFinder(this).where(predicate).findAll(includeInvisible)
}

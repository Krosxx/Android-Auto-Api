@file:Suppress("unused")

package cn.vove7.andro_accessibility_api.viewfinder

import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import cn.vove7.andro_accessibility_api.utils.compareSimilarity

/**
 * # SmartFinderConditions
 *
 * @author Libra
 * @date 2022/2/15
 */

typealias AcsNode = AccessibilityNodeInfo

private fun requireNotEmpty(list: Array<*>) {
    if (list.isEmpty()) throw IllegalStateException("requireNotEmpty")
}

class IdCondition(private val targetId: String) : MatchCondition {
    override fun invoke(node: AccessibilityNodeInfo): Boolean {
        val vid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            node.viewIdResourceName ?: return false
        } else {
            return false
        }
        return vid.endsWith("/$targetId", true) || vid.equals(targetId, true)
    }
}

fun ConditionGroup.id(id: String) = link(IdCondition(id))

class TextEqCondition(private val texts: Array<out String>) : MatchCondition {
    init {
        requireNotEmpty(texts)
    }

    override fun invoke(node: AcsNode) = node.text?.toString()?.let {
        texts.any { t -> t == it }
    } ?: false
}

fun ConditionGroup.text(vararg texts: String) = link(TextEqCondition(texts))

object _id {
    operator fun rangeTo(s: String) = _desc.eq(s)
    infix fun eq(s: String): MatchCondition = IdCondition(s)
}

object _desc {
    operator fun rangeTo(s: String) = eq(s)
    infix fun eq(s: String): MatchCondition = DescEqCondition(arrayOf(s))
    infix fun contains(s: String): MatchCondition = ContainDescCondition(arrayOf(s))
}

object _text {
    operator fun rangeTo(s: String) = _desc.eq(s)
    infix fun eq(s: String): MatchCondition = TextEqCondition(arrayOf(s))
    infix fun contains(s: String): MatchCondition = ContainTextCondition(arrayOf(s))
    infix fun match(s: String): MatchCondition = RTextEqCondition(s)
}

abstract class RegexCondition(private val regex: String) : MatchCondition {
    abstract fun AcsNode.nodeText(): String?
    private val reg = regex.toRegex()

    override fun invoke(node: AcsNode) =
        node.nodeText()?.let {
            reg.matches(it)
        } ?: false
}

class RTextEqCondition(regex: String) : RegexCondition(regex) {
    override fun AcsNode.nodeText(): String? = text?.toString()
}

fun ConditionGroup.matchText(reg: String) = link(RTextEqCondition(reg))

class ContainTextCondition(private val texts: Array<out String>) : MatchCondition {
    init {
        requireNotEmpty(texts)
    }

    override fun invoke(node: AcsNode) = node.text?.toString()?.let {
        texts.any { t -> it.contains(t) }
    } ?: false
}

fun ConditionGroup.containsText(vararg texts: String) = link(ContainTextCondition(texts))

class SimilarityTextCondition(
    private val text: String,
    private val limit: Float
) : MatchCondition {
    override fun invoke(node: AccessibilityNodeInfo): Boolean {
        return compareSimilarity(node.text?.toString() ?: "", text) >= limit
    }
}

fun ConditionGroup.similarityText(text: String, limit: Float) =
    link(SimilarityTextCondition(text, limit))

class SimilarityDescCondition(
    private val text: String,
    private val limit: Float
) : MatchCondition {
    override fun invoke(node: AccessibilityNodeInfo): Boolean {
        return compareSimilarity(node.contentDescription?.toString() ?: "", text) >= limit
    }
}

fun ConditionGroup.similarityDesc(text: String, limit: Float) =
    link(SimilarityDescCondition(text, limit))

class DescEqCondition(private val texts: Array<out String>) : MatchCondition {
    init {
        requireNotEmpty(texts)
    }

    override fun invoke(node: AcsNode) = texts.any {
        return node.contentDescription?.toString() == it
    }
}

fun ConditionGroup.desc(vararg desc: String) = link(DescEqCondition(desc))

class ContainDescCondition(private val texts: Array<out String>) : MatchCondition {
    init {
        requireNotEmpty(texts)
    }

    override fun invoke(node: AcsNode) = node.contentDescription?.toString()?.let {
        texts.any { t -> it.contains(t) }
    } ?: false
}

fun ConditionGroup.containsDesc(vararg desc: String) = link(ContainDescCondition(desc))

class TextOrDescEqCondition(private val texts: Array<out String>) : MatchCondition {
    init {
        requireNotEmpty(texts)
    }

    override fun invoke(node: AcsNode): Boolean {
        return TextEqCondition(texts)(node) || DescEqCondition(texts)(node)
    }
}

fun ConditionGroup.textOrDesc(vararg texts: String) = link(TextOrDescEqCondition(texts))

abstract class BoolCondition(private val b: Boolean) : MatchCondition {
    abstract fun AcsNode.prop(): Boolean?
    override fun invoke(node: AcsNode) = node.prop() == b
}

class ClickableCondition(b: Boolean) : BoolCondition(b) {
    override fun AcsNode.prop() = isClickable
}

@JvmOverloads
fun ConditionGroup.clickable(b: Boolean = true) = link(ClickableCondition(b))

class CheckableCondition(b: Boolean) : BoolCondition(b) {
    override fun AcsNode.prop() = isCheckable
}

@JvmOverloads
fun ConditionGroup.checkable(b: Boolean = true) = link(CheckableCondition(b))

class CheckedCondition(b: Boolean) : BoolCondition(b) {
    override fun AcsNode.prop() = isChecked
}

@JvmOverloads
fun ConditionGroup.checked(b: Boolean = true) = link(CheckedCondition(b))

class DismissableCondition(private val b: Boolean) : MatchCondition {
    override fun invoke(node: AcsNode) =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                && node.isDismissable == b
}

@JvmOverloads
fun ConditionGroup.dismissable(b: Boolean = true) = link(DismissableCondition(b))


class EditableCondition(b: Boolean) : BoolCondition(b) {
    override fun AcsNode.prop(): Boolean? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) return null
        return isEditable
    }
}

@JvmOverloads
fun ConditionGroup.editable(b: Boolean = true) = link(EditableCondition(b))

class LongClickableCondition(b: Boolean) : BoolCondition(b) {
    override fun AcsNode.prop() = isLongClickable
}

@JvmOverloads
fun ConditionGroup.longClickable(b: Boolean = true) = link(LongClickableCondition(b))


class EnabledCondition(private val b: Boolean) : MatchCondition {
    override fun invoke(node: AcsNode) = node.isEnabled == b
}

@JvmOverloads
fun ConditionGroup.enabled(b: Boolean = true) = link(EnabledCondition(b))


class FocusableCondition(private val b: Boolean) : MatchCondition {
    override fun invoke(node: AcsNode) = node.isFocusable == b
}

@JvmOverloads
fun ConditionGroup.focusable(b: Boolean = true) = link(FocusableCondition(b))

class FocusedCondition(b: Boolean = true) : BoolCondition(b) {
    override fun AcsNode.prop() = isFocused
}

@JvmOverloads
fun ConditionGroup.focused(b: Boolean = true) = link(FocusedCondition(b))

object HasChildCondition : MatchCondition {
    override fun invoke(node: AcsNode) = node.childCount > 0
}

fun ConditionGroup.hasChild() = link(HasChildCondition)

object NoChildCondition : MatchCondition {
    override fun invoke(node: AcsNode) = node.childCount == 0
}

fun ConditionGroup.noChild() = link(NoChildCondition)

class ClassNameCondition(private val clses: Array<out String>) : MatchCondition {
    init {
        requireNotEmpty(clses)
    }

    override fun invoke(node: AcsNode): Boolean {
        val clsName = node.className?.toString() ?: return false
        return clses.any { clsName.contains(it, ignoreCase = true) }
    }
}

fun ConditionGroup.type(vararg types: String) = link(ClassNameCondition(types))

class ScrollableCondition(b: Boolean) : BoolCondition(b) {
    override fun AcsNode.prop() = isScrollable
}

@JvmOverloads
fun ConditionGroup.scrollable(b: Boolean = true) = link(ScrollableCondition(b))


class SelectedCondition(b: Boolean) : BoolCondition(b) {
    override fun AcsNode.prop() = isSelected
}

@JvmOverloads
fun ConditionGroup.selected(b: Boolean = true) = link(SelectedCondition(b))

//
//class CCondition(private val b: Boolean) : MatchCondition {
//    override fun invoke(node: AcsNode): Boolean {
//
//        node.className
//
//        return false
//    }
//}

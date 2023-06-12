@file:Suppress("unused")

package cn.vove7.auto.core.viewfinder

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import cn.vove7.auto.core.utils.compareSimilarity

/**
 * # SmartFinderConditions
 *
 * @author Vove
 * @date 2022/2/15
 */

typealias AcsNode = AccessibilityNodeInfoCompat

private fun requireNotEmpty(list: Array<*>) {
    if (list.isEmpty()) throw IllegalStateException("requireNotEmpty")
}

class IdCondition(private val targetId: String) : MatchCondition {
    override fun invoke(node: AcsNode): Boolean {
        val vid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            node.viewIdResourceName ?: return false
        } else {
            return false
        }
        return vid.endsWith("/$targetId", true) || vid.equals(targetId, true)
    }

    override fun toString() = "ID == $targetId"
}

fun ConditionGroup.id(id: String) = link(IdCondition(id))
fun id(id: String) = IdCondition(id)

class IdSCondition(private val targetIds: Array<out String>) : MatchCondition {
    override fun invoke(node: AcsNode): Boolean {
        val vid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            node.viewIdResourceName ?: return false
        } else {
            return false
        }
        return targetIds.any { id ->
            vid.endsWith("/$id", true) || vid.equals(id, true)
        }
    }

    override fun toString() = "ID in ${targetIds.contentToString()}"
}

fun ConditionGroup.ids(vararg id: String) = link(IdSCondition(id))


class PackageCondition(private val names: Array<out CharSequence>) : MatchCondition {
    override fun invoke(node: AcsNode): Boolean {
        return node.packageName in names
    }
}

fun ConditionGroup.packageName(vararg packageNames: CharSequence) = link(PackageCondition(packageNames))


class TextEqCondition(private val texts: Array<out String>) : MatchCondition {
    init {
        requireNotEmpty(texts)
    }

    override fun invoke(node: AcsNode) = node.text?.toString()?.let {
        texts.any { t -> t.equals(it, ignoreCase = true) }
    } ?: false

    override fun toString() = if (texts.size == 1)
        "TEXT == ${texts.first()}"
    else "TEXT in ${texts.contentToString()}"

}

fun ConditionGroup.text(vararg texts: String) = link(TextEqCondition(texts))
fun text(vararg texts: String) = TextEqCondition(texts)

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

abstract class RegexCondition(regex: String) : MatchCondition {
    abstract fun AcsNode.nodeText(): String?
    internal val reg = regex.toRegex()

    override fun invoke(node: AcsNode) =
        node.nodeText()?.let {
            reg.matches(it)
        } ?: false
}

class RTextEqCondition(regex: String) : RegexCondition(regex) {
    override fun AcsNode.nodeText(): String? = text?.toString()

    override fun toString() = "${reg.pattern} matches(TEXT)"
}

fun ConditionGroup.matchText(reg: String) = link(RTextEqCondition(reg))
fun matchText(reg: String) = RTextEqCondition(reg)

class ContainTextCondition(private val texts: Array<out String>) : MatchCondition {
    init {
        requireNotEmpty(texts)
    }

    override fun invoke(node: AcsNode) = node.text?.toString()?.let {
        texts.any { t -> it.contains(t, ignoreCase = true) }
    } ?: false

    override fun toString() = if (texts.size == 1)
        "TEXT contains ${texts.first()}"
    else "TEXT any contains ${texts.contentToString()}"
}

fun ConditionGroup.containsText(vararg texts: String) = link(ContainTextCondition(texts))
fun containsText(vararg texts: String) = ContainTextCondition(texts)

class SimilarityTextCondition(
    private val text: String,
    private val limit: Float
) : MatchCondition {
    override fun invoke(node: AcsNode): Boolean {
        return compareSimilarity(node.text?.toString() ?: "", text) >= limit
    }

    override fun toString() = "TEXT like $text >= $limit"
}

fun ConditionGroup.similarityText(text: String, limit: Float) =
    link(SimilarityTextCondition(text, limit))

fun similarityText(text: String, limit: Float) = SimilarityTextCondition(text, limit)

class SimilarityDescCondition(
    private val text: String,
    private val limit: Float
) : MatchCondition {
    override fun invoke(node: AcsNode): Boolean {
        return compareSimilarity(node.contentDescription?.toString() ?: "", text) >= limit
    }

    override fun toString() = "DESC like $text >= $limit"
}

fun ConditionGroup.similarityDesc(text: String, limit: Float) =
    link(SimilarityDescCondition(text, limit))

fun similarityDesc(text: String, limit: Float) = SimilarityDescCondition(text, limit)

class DescEqCondition(private val texts: Array<out String>) : MatchCondition {
    init {
        requireNotEmpty(texts)
    }

    override fun invoke(node: AcsNode) = texts.any {
        it.equals(node.contentDescription?.toString(), ignoreCase = true)
    }

    override fun toString() = if (texts.size == 1)
        "DESC == ${texts.first()}"
    else "DESC in ${texts.contentToString()}"
}

fun ConditionGroup.desc(vararg desc: String) = link(DescEqCondition(desc))
fun desc(vararg desc: String) = DescEqCondition(desc)

class ContainDescCondition(private val texts: Array<out String>) : MatchCondition {
    init {
        requireNotEmpty(texts)
    }

    override fun invoke(node: AcsNode) = node.contentDescription?.toString()?.let {
        texts.any { t -> it.contains(t) }
    } ?: false

    override fun toString() = if (texts.size == 1)
        "DESC contains ${texts.first()}"
    else "DESC any contains ${texts.contentToString()}"
}

fun ConditionGroup.containsDesc(vararg desc: String) = link(ContainDescCondition(desc))
fun containsDesc(vararg desc: String) = ContainDescCondition(desc)

class TextOrDescEqCondition(private val texts: Array<out String>) : MatchCondition {
    init {
        requireNotEmpty(texts)
    }

    private val tm = TextEqCondition(texts)
    private val dm = DescEqCondition(texts)

    override fun invoke(node: AcsNode): Boolean {
        return tm(node) || dm(node)
    }

    override fun toString() = "($tm || $dm)"
}

fun ConditionGroup.textOrDesc(vararg texts: String) = link(TextOrDescEqCondition(texts))
fun textOrDesc(vararg texts: String) = TextOrDescEqCondition(texts)

abstract class BoolCondition(internal val b: Boolean) : MatchCondition {
    abstract fun AcsNode.prop(): Boolean?
    override fun invoke(node: AcsNode) = node.prop() == b
}

class ClickableCondition(b: Boolean) : BoolCondition(b) {
    override fun AcsNode.prop() = isClickable

    override fun toString() = if (b) "Clickable" else "NotClickable"
}

@JvmOverloads
fun ConditionGroup.clickable(b: Boolean = true) = link(ClickableCondition(b))

@JvmOverloads
fun clickable(b: Boolean = true) = ClickableCondition(b)

class CheckableCondition(b: Boolean) : BoolCondition(b) {
    override fun AcsNode.prop() = isCheckable
    override fun toString() = if (b) "Checkable" else "NotCheckable"
}

@JvmOverloads
fun ConditionGroup.checkable(b: Boolean = true) = link(CheckableCondition(b))

@JvmOverloads
fun checkable(b: Boolean = true) = CheckableCondition(b)

class CheckedCondition(b: Boolean) : BoolCondition(b) {
    override fun AcsNode.prop() = isChecked
    override fun toString() = if (b) "Checked" else "NotChecked"
}

@JvmOverloads
fun ConditionGroup.checked(b: Boolean = true) = link(CheckedCondition(b))

@JvmOverloads
fun checked(b: Boolean = true) = CheckedCondition(b)

@Suppress("SpellCheckingInspection")
@RequiresApi(Build.VERSION_CODES.KITKAT)
class DismissableCondition(b: Boolean) : BoolCondition(b) {
    override fun AcsNode.prop() = isDismissable

    override fun toString() = if (b) "Dismissable" else "NotDismissable"
}

@Suppress("SpellCheckingInspection")
@RequiresApi(Build.VERSION_CODES.KITKAT)
@JvmOverloads
fun ConditionGroup.dismissable(b: Boolean = true) = link(DismissableCondition(b))

@Suppress("SpellCheckingInspection")
@RequiresApi(Build.VERSION_CODES.KITKAT)
@JvmOverloads
fun dismissable(b: Boolean = true) = DismissableCondition(b)


@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class EditableCondition(b: Boolean) : BoolCondition(b) {
    override fun AcsNode.prop(): Boolean = isEditable
    override fun toString() = if (b) "Editable" else "NotEditable"
}

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
@JvmOverloads
fun ConditionGroup.editable(b: Boolean = true) = link(EditableCondition(b))

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
@JvmOverloads
fun editable(b: Boolean = true) = EditableCondition(b)

class LongClickableCondition(b: Boolean) : BoolCondition(b) {
    override fun AcsNode.prop() = isLongClickable
    override fun toString() = if (b) "LongClickable" else "NotLongClickable"
}

@JvmOverloads
fun ConditionGroup.longClickable(b: Boolean = true) = link(LongClickableCondition(b))

@JvmOverloads
fun longClickable(b: Boolean = true) = LongClickableCondition(b)


class EnabledCondition(b: Boolean) : BoolCondition(b) {
    override fun AcsNode.prop() = isEnabled
    override fun toString() = if (b) "Enabled" else "NotEnabled"
}

@JvmOverloads
fun ConditionGroup.enabled(b: Boolean = true) = link(EnabledCondition(b))

@JvmOverloads
fun enabled(b: Boolean = true) = EnabledCondition(b)


class FocusableCondition(b: Boolean) : BoolCondition(b) {
    override fun AcsNode.prop() = isFocusable
    override fun toString() = if (b) "Focusable" else "NotFocusable"
}

@JvmOverloads
fun ConditionGroup.focusable(b: Boolean = true) = link(FocusableCondition(b))

@JvmOverloads
fun focusable(b: Boolean = true) = FocusableCondition(b)

class FocusedCondition(b: Boolean = true) : BoolCondition(b) {
    override fun AcsNode.prop() = isFocused
    override fun toString() = if (b) "Focused" else "NotFocused"
}

@JvmOverloads
fun ConditionGroup.focused(b: Boolean = true) = link(FocusedCondition(b))

@JvmOverloads
fun focused(b: Boolean = true) = FocusedCondition(b)

object HasChildCondition : MatchCondition {
    override fun invoke(node: AcsNode) = node.childCount > 0
    override fun toString() = "HasChild"
}

fun ConditionGroup.hasChild() = link(HasChildCondition)

fun hasChild() = HasChildCondition

object NoChildCondition : MatchCondition {
    override fun invoke(node: AcsNode) = node.childCount == 0
    override fun toString() = "NoChild"
}

fun ConditionGroup.noChild() = link(NoChildCondition)
fun noChild() = NoChildCondition

class ClassNameCondition(private val clses: Array<out String>) : MatchCondition {
    init {
        requireNotEmpty(clses)
    }

    override fun invoke(node: AcsNode): Boolean {
        val clsName = node.className?.toString() ?: return false
        return clses.any { clsName.contains(it, ignoreCase = true) }
    }

    override fun toString() = "CLASSNAME like ${clses.contentToString()}"
}

fun ConditionGroup.type(vararg types: String) = link(ClassNameCondition(types))
fun type(vararg types: String) = ClassNameCondition(types)

class ScrollableCondition(b: Boolean) : BoolCondition(b) {
    override fun AcsNode.prop() = isScrollable
    override fun toString() = if (b) "Scrollable" else "NotScrollable"

}

@JvmOverloads
fun ConditionGroup.scrollable(b: Boolean = true) = link(ScrollableCondition(b))
fun scrollable(b: Boolean = true) = ScrollableCondition(b)


class SelectedCondition(b: Boolean) : BoolCondition(b) {
    override fun AcsNode.prop() = isSelected
    override fun toString() = if (b) "Selected" else "NotSelected"
}

@JvmOverloads
fun ConditionGroup.selected(b: Boolean = true) = link(SelectedCondition(b))

@JvmOverloads
fun selected(b: Boolean = true) = SelectedCondition(b)

//
// class CCondition(private val b: Boolean) : MatchCondition {
//    override fun invoke(node: AcsNode): Boolean {
//
//        node.className
//
//        return false
//    }
//}

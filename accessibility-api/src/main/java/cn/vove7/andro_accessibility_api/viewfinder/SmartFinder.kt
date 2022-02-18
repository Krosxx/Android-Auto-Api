package cn.vove7.andro_accessibility_api.viewfinder

import android.view.accessibility.AccessibilityNodeInfo
import cn.vove7.andro_accessibility_api.viewnode.ViewNode
import kotlin.coroutines.CoroutineContext

/**
 * # SmartFinder
 * 具备扩展性
 *
 *
 * @author Libra
 * @date 2022/2/15
 */

typealias MatchCondition = (node: AccessibilityNodeInfo) -> Boolean

interface Condition {
    var type: ConditionType

    fun check(node: AccessibilityNodeInfo): Boolean
}

enum class ConditionType { AND, OR }

class ConditionNode(
    override var type: ConditionType,
    val condition: MatchCondition
) : Condition {
    override fun check(node: AccessibilityNodeInfo) = condition(node)
}

interface ConditionGroup : Condition, ViewFinder<ConditionGroup>, FinderBuilderWithOperation {

    val conditions: MutableList<Condition>
    var lastType: ConditionType

    fun and(vararg conditions: MatchCondition): ConditionGroup {
        if (conditions.isEmpty()) {
            lastType = ConditionType.AND
            return this
        }
        this.conditions.addAll(conditions.map {
            ConditionNode(ConditionType.AND, it)
        })
        return this
    }

    fun link(cond: MatchCondition) =
        if (lastType == ConditionType.AND) {
            and(cond)
        } else or(cond)

    infix fun and(cond: MatchCondition): ConditionGroup {
        this.conditions.add(ConditionNode(ConditionType.AND, cond))
        return this
    }

    infix fun or(cond: MatchCondition): ConditionGroup {
        this.conditions.add(ConditionNode(ConditionType.OR, cond))
        return this
    }

    fun or(vararg conditions: MatchCondition): ConditionGroup {
        if (conditions.isEmpty()) {
            lastType = ConditionType.OR
            return this
        }
        this.conditions.addAll(conditions.map {
            ConditionNode(ConditionType.OR, it)
        })
        return this
    }

    infix fun and(group: ConditionGroup): ConditionGroup {
        group.type = ConditionType.AND
        conditions.add(group)
        return this
    }

    infix fun or(group: ConditionGroup): ConditionGroup {
        group.type = ConditionType.OR
        conditions.add(group)
        return this
    }

    override fun check(node: AccessibilityNodeInfo): Boolean {
        if (conditions.isEmpty()) {
            throw IllegalArgumentException("SmartFinder has no conditions")
        }
        if (conditions.first().type == ConditionType.OR) {
            throw IllegalStateException("first condition type must be AND")
        }
        conditions.forEachIndexed { i, cond ->
            if (cond.check(node)) {
                if (i + 1 < conditions.size && conditions[i + 1].type == ConditionType.OR) {
                    //break true or (..)
                    return true
                }
                return@forEachIndexed
            } else if (i + 1 < conditions.size && conditions[i + 1].type == ConditionType.OR) {
                return@forEachIndexed
            } else return false
        }
        // all pass
        return true
    }

    infix fun where(cond: MatchCondition): ConditionGroup = and(cond)
    infix fun where(group: ConditionGroup): ConditionGroup = and(group)
}

fun SG(vararg conditions: MatchCondition) = SF.apply {
    and(*conditions)
}

fun SG(group: ConditionGroup) = SF.apply {
    and(group)
}

val SF get() = SmartFinder()

class SmartFinder(
    override val node: ViewNode? = null
) : ConditionGroup {
    override var coroutineCtx: CoroutineContext? = null

    override var lastType: ConditionType = ConditionType.AND

    override var finder: ViewFinder<*> = this

    override fun findCondition(node: AccessibilityNodeInfo) = check(node)

    override var type: ConditionType = ConditionType.AND

    override val conditions = mutableListOf<Condition>()

    operator fun invoke() = this
}
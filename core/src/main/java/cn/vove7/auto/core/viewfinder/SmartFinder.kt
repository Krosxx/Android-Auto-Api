package cn.vove7.auto.core.viewfinder

import cn.vove7.auto.core.viewnode.ViewNode
import java.util.concurrent.atomic.AtomicBoolean

/**
 * # SmartFinder
 * 高扩展性
 * 多条件搜索
 *
 * @author Vove
 * @date 2022/2/15
 */
fun interface MatchCondition {
    var conditionType: ConditionType
        get() = ConditionType.AND
        set(_) {}

    val canInterrupt: Boolean get() = false

    // AcsNode 条件匹配
    // interrupt: 中断子结点查询
    // 值为 true 时，当 invoke 返回false将中止 children 查询
    fun match(node: AcsNode, interrupt: AtomicBoolean) = match(node).also {
        interrupt.set(canInterrupt && !it)
    }

    fun match(node: AcsNode): Boolean
}

enum class ConditionType { AND, OR }

class ConditionNode(
    override var conditionType: ConditionType,
    val condition: MatchCondition
) : MatchCondition by condition {
    override fun toString() = condition.toString()
}

open class ConditionGroup(
    node: ViewNode? = null
) : ViewFinder<ConditionGroup>(node), MatchCondition, FinderBuilderWithOperation {
    override fun finderInfo(): String = "ConditionGroup${toString()}"

    override var conditionType: ConditionType = ConditionType.AND
    internal val conditions: MutableList<MatchCondition> = mutableListOf()
    private var lastType: ConditionType = ConditionType.AND
    override val finder: ViewFinder<*> get() = this

    override fun toString() = buildString {
        append("(")
        conditions.forEachIndexed { i, cond ->
            if (i > 0) {
                append(if (cond.conditionType == ConditionType.AND) " && " else " || ")
            }
            append(cond.toString())
        }
        append(")")
    }

    override fun findCondition(node: AcsNode, interrupt: AtomicBoolean):
            Boolean = match(node, interrupt)

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

    fun link(vararg cond: MatchCondition) =
        if (lastType == ConditionType.AND) {
            and(*cond)
        } else or(*cond)

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
        group.conditionType = ConditionType.AND
        conditions.add(group)
        return this
    }

    infix fun or(group: ConditionGroup): ConditionGroup {
        group.conditionType = ConditionType.OR
        conditions.add(group)
        return this
    }

    override fun match(node: AcsNode): Boolean {
        error("SF Don't want to enter here [match]")
    }

    override fun match(node: AcsNode, interrupt: AtomicBoolean): Boolean {
        if (conditions.isEmpty()) {
            throw IllegalArgumentException("SmartFinder has no conditions")
        }
        if (conditions.first().conditionType == ConditionType.OR) {
            throw IllegalStateException("first condition type must be AND")
        }
        conditions.forEachIndexed { i, cond ->
            if (cond.match(node, interrupt)) {
                if (i + 1 < conditions.size && conditions[i + 1].conditionType == ConditionType.OR) {
                    // break true or (..)
                    return true
                }
                return@forEachIndexed
            } else if (i + 1 < conditions.size && conditions[i + 1].conditionType == ConditionType.OR) {
                return@forEachIndexed
            } else return false
        }
        // all pass
        return true
    }

    infix fun where(cond: MatchCondition): ConditionGroup = and(cond)
    infix fun where(group: ConditionGroup): ConditionGroup = and(group)

    fun where(tag: String? = null, cond: MatchCondition): ConditionGroup = and(
        LambdaCondition(cond, tag)
    )

    fun where(tag: String? = null, group: ConditionGroup): ConditionGroup = and(
        LambdaCondition(group, tag)
    )
}

private class LambdaCondition(cond: MatchCondition, tag: String?) : MatchCondition by cond {
    private val _tag = tag ?: cond.toString()
    override fun toString() = _tag
}

val SF get() = SmartFinder()

class SmartFinder(
    node: ViewNode? = null
) : ConditionGroup(node) {
    operator fun invoke(vararg conditions: MatchCondition) = link(*conditions)

    override fun finderInfo(): String {
        return "SmartFinder startNode: ${node?.toString() ?: "root"} " +
                "Conditions: ${super.toString()}"
    }
}
package cn.vove7.andro_accessibility_api.viewfinder

import android.view.accessibility.AccessibilityNodeInfo
import cn.vove7.andro_accessibility_api.AccessibilityApi
import cn.vove7.andro_accessibility_api.utils.NeedBaseAccessibilityException
import cn.vove7.andro_accessibility_api.viewnode.ViewNode

/**
 * 查找符合条件的AccessibilityNodeInfo
 * @param node 开始节点
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class ViewFinder(val node: ViewNode?) {

    companion object {
        //Maximum layout recursion depth; in rare cases, there will be a layout wireless loop to prevent function stack overflow
        //最大布局递归深度；极少情况会出现布局无线循环，用来防止函数栈溢出
        var MAX_DEPTH = 50
    }

    val startNode: ViewNode
        get() = node ?: run {
            val service = AccessibilityApi.baseService
                ?: throw NeedBaseAccessibilityException()
            service.rootNodeOfAllWindows
        }

    /**
     * 等待搜索，在指定时间内循环搜索（视图更新），超时返回null
     * 等待View出现 同步 耗时操作
     * 主动搜索
     * @param m Long 时限
     */
    fun waitFor(m: Long = 30000, includeInvisible: Boolean = false): ViewNode? {
        if (!AccessibilityApi.isBaseServiceEnable) throw NeedBaseAccessibilityException()
        val t = when {
            m in 0..30000 -> m
            m < 0 -> 0
            else -> 30000
        }
        val beginTime = System.currentTimeMillis()
        var sc = 0
        val ct = Thread.currentThread()
        val endTime = beginTime + t
        while (System.currentTimeMillis() < endTime &&
            !ct.isInterrupted
        ) {
            val node = findFirst(includeInvisible)
            if (node != null) {
                return node
            } else {
                sc++
            }
        }
        return null
    }

    /**
     * @param includeInvisible Boolean 是否包含不可见元素
     * @return ViewNode?
     */
    open fun findFirst(includeInvisible: Boolean = false): ViewNode? {
        //不可见
        return traverseAllNode(startNode, includeInvisible = includeInvisible)
    }

    private val list = mutableListOf<ViewNode>()

    //[findAll]
    fun find(includeInvisible: Boolean = false): Array<ViewNode> {
        return findAll(includeInvisible)
    }

    /**
     *
     * @param includeInvisible Boolean 是否包含不可见元素
     * @return Array<ViewNode> 无结果则返回空
     */
    @JvmOverloads
    fun findAll(includeInvisible: Boolean = false): Array<ViewNode> {
        list.clear()
        traverseAllNode(startNode, true, includeInvisible)
        val l = mutableListOf<ViewNode>()
        l.addAll(list)
        return l.toTypedArray()
    }

    /**
     * 深搜遍历
     *
     * @param node AccessibilityNodeInfo?
     * @param all Boolean true 搜索全部返回list else return first
     * @param includeInvisible Boolean 是否包含不可见元素
     * @return ViewNode?
     */
    private fun traverseAllNode(
        node: ViewNode?, all: Boolean = false,
        includeInvisible: Boolean = false, depth: Int = 0
    ): ViewNode? {
        node ?: return null
        if (depth > MAX_DEPTH) {//防止出现无限递归（eg:QQ浏览器首页）
            return null
        }
        node.children.forEach { childNode ->
            if (!includeInvisible && !childNode.isVisibleToUser) {
                return@forEach
            }
            if (findCondition(childNode.node)) {
                if (all) {
                    list.add(childNode)
                } else return childNode
            }
            val r = traverseAllNode(childNode, all, includeInvisible, depth + 1)
            if (!all && r != null) {
                return r
            }
        }
        return null
    }

    /**
     * 查找条件
     */
    abstract fun findCondition(node: AccessibilityNodeInfo): Boolean

}
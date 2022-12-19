package cn.vove7.andro_accessibility_api.viewfinder

import android.view.accessibility.AccessibilityNodeInfo
import cn.vove7.andro_accessibility_api.api.requireBaseAccessibility
import cn.vove7.andro_accessibility_api.utils.ViewNodeNotFoundException
import cn.vove7.andro_accessibility_api.utils.ensureActive
import cn.vove7.andro_accessibility_api.utils.whileWaitTime
import cn.vove7.andro_accessibility_api.viewfinder.FinderBuilderWithOperation.Companion.WAIT_MILLIS
import cn.vove7.andro_accessibility_api.viewnode.ViewNode
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/**
 * 查找符合条件的AccessibilityNodeInfo
 * @param node 开始节点
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class ViewFinder<T : ViewFinder<T>>(
    val node: ViewNode? = null
) {

    companion object {
        //Maximum layout recursion depth; in rare cases, there will be a layout wireless loop to prevent function stack overflow
        //最大布局递归深度；极少情况会出现布局无线循环，用来防止函数栈溢出
        var MAX_DEPTH = 50

        val ROOT_NODE: ViewNode get() = ViewNode.getRoot()

        /**
         * 使用深度搜索
         * @param depths Array<Int>
         * @return ViewNode?
         */
        suspend fun findByDepths(depths: IntArray, node: ViewNode): ViewNode? {
            var p: ViewNode? = node
            depths.forEach {
                ensureActive()
                try {
                    p = p?.childAt(it)
                } catch (e: ArrayIndexOutOfBoundsException) {
                    return null
                }
                if (p == null) {
                    return null
                }
            }
            return p
        }

        suspend fun findByDepths(vararg depths: Int) = findByDepths(depths, ROOT_NODE)
    }

    val startNode: ViewNode
        get() = node ?: ROOT_NODE

    /**
     * 等待搜索，在指定时间内循环搜索（视图更新），超时返回null
     * 等待View出现 同步 耗时操作
     * 主动搜索
     * @param waitTime Long 时限
     */
    suspend fun waitFor(
        waitTime: Long = 30000,
        interval: Long = 0L,
        includeInvisible: Boolean = false
    ): ViewNode? {
        requireBaseAccessibility()
        val wt = when {
            waitTime in 0..30000 -> waitTime
            waitTime < 0 -> 0
            else -> 30000
        }
        val beginTime = System.currentTimeMillis()
        val endTime = beginTime + wt
        do {
            val node = findFirst(includeInvisible)
            if (node != null) return node
            if (interval > 0) delay(interval)
            else ensureActive()
        } while (System.currentTimeMillis() < endTime)
        return null
    }

    /**
     * @param includeInvisible Boolean 是否包含不可见元素
     * @return ViewNode?
     */
    suspend fun findFirst(includeInvisible: Boolean = false): ViewNode? {
        //不可见
        return traverseAllNode(startNode, includeInvisible = includeInvisible)
    }

    /**
     * 使用深度搜索
     * @param depths Array<Int>
     * @return ViewNode?
     */
    suspend fun findByDepths(vararg depths: Int): ViewNode? {
        return Companion.findByDepths(depths, startNode)
    }

    //[findAll]
    suspend fun find(includeInvisible: Boolean = false) = findAll(includeInvisible)

    @Throws(ViewNodeNotFoundException::class)
    suspend fun require(waitMillis: Long = WAIT_MILLIS): ViewNode {
        return waitFor(waitMillis) ?: throw ViewNodeNotFoundException(this)
    }

    suspend fun exist(): Boolean = findFirst() != null

    /**
     *
     * @param includeInvisible Boolean 是否包含不可见元素
     * @return Array<ViewNode> 无结果则返回空
     */
    suspend fun findAll(includeInvisible: Boolean = false): Array<ViewNode> {
        val l = mutableListOf<ViewNode>()
        traverseAllNode(startNode, l, includeInvisible)
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
    private suspend fun traverseAllNode(
        node: ViewNode?, list: MutableList<ViewNode>? = null,
        includeInvisible: Boolean = false, depth: Int = 0
    ): ViewNode? {
        ensureActive()
        node ?: return null
        if (depth > MAX_DEPTH) {//防止出现无限递归（eg:QQ浏览器首页）
            return null
        }
        node.children.forEach { childNode ->
            ensureActive()
            if (!includeInvisible && !childNode.isVisibleToUser) {
                return@forEach
            }
            if (findCondition(childNode.node)) {
                if (list != null) {
                    list.add(childNode)
                } else return childNode
            }
            val r = traverseAllNode(childNode, list, includeInvisible, depth + 1)
            if (list == null && r != null) {
                return r
            }
        }
        return null
    }

    /**
     * 默认10s等待时间
     * @return Boolean
     */
    suspend fun waitHide(): Boolean {
        return waitHide(10000)
    }

    /**
     * 等待消失  常用于加载View的消失
     * @param waitMs max 60s
     * @return Boolean false 超时 true 消失
     */
    suspend fun waitHide(waitMs: Int, interval: Long = 0L): Boolean {
        return whileWaitTime(waitMs.toLong(), interval) {
            ensureActive()
            if (findFirst() != null) {
                null
            }//显示，继续等待
            else {
                true
            } //消失
        } ?: false
    }

    suspend fun await() = waitFor()

    suspend fun await(l: Long): ViewNode? = waitFor(l)

    /**
     * 查找条件
     */
    abstract fun findCondition(node: AccessibilityNodeInfo): Boolean

    abstract fun finderInfo(): String
}
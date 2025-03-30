package cn.vove7.auto.core.viewfinder

import cn.vove7.auto.core.utils.ViewNodeNotFoundException
import cn.vove7.auto.core.utils.ensureActive
import cn.vove7.auto.core.utils.ensureNotInterrupt
import cn.vove7.auto.core.utils.whileWaitTime
import cn.vove7.auto.core.viewnode.ViewNode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * 查找符合条件的AccessibilityNodeInfo
 * @param node 开始节点
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class ViewFinder<T : ViewFinder<T>>(
    val node: ViewNode? = null
) {

    val startNode: ViewNode
        get() = node ?: ViewNode.getRoot()

    // startNode 为 ViewNode.getRoot() 时，SDK_INT >= LOLLIPOP 搜索失败时，
    // 走 SDK_INT < LOLLIPOP root 节点
    private var rootCompat: Boolean = false
        get() = field && this.node == null

    /**
     * 等待搜索，在指定时间内循环搜索（视图更新），超时返回null
     * 等待View出现 同步 耗时操作
     * 主动搜索
     * @param waitTime Long 时限
     */
    suspend fun waitFor(
        waitTime: Long = FinderConfig.FINDER_WAIT_MILLIS,
        interval: Long = FinderConfig.FINDER_WAIT_INTERVAL,
        includeInvisible: Boolean = false
    ): ViewNode? {
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
            if (FinderConfig.ENABLE_FIND_FAILED_STRATEGY) {
                FinderConfig.onFindFailed?.invoke(this)
            }
            if (interval > 0) delay(interval)
            else ensureActive()
        } while (System.currentTimeMillis() < endTime)
        return null
    }

    @Throws(ViewNodeNotFoundException::class)
    suspend fun requireFirst(includeInvisible: Boolean = false): ViewNode {
        return findFirst(includeInvisible) ?: throw ViewNodeNotFoundException(this)
    }

    @Throws(ViewNodeNotFoundException::class)
    fun requireFirstBlocking(includeInvisible: Boolean = false): ViewNode {
        return findFirstBlocking(includeInvisible) ?: throw ViewNodeNotFoundException(this)
    }


    /**
     * @param includeInvisible Boolean 是否包含不可见元素
     * @return ViewNode?
     */
    suspend fun findFirst(includeInvisible: Boolean = false): ViewNode? {
        // 不可见
        return traverseAllNode(startNode, includeInvisible = includeInvisible).let {
            if (it == null && rootCompat) {
                traverseAllNode(ViewNode.activeWinNode(), includeInvisible = includeInvisible)
            } else it
        }
    }

    @Throws(CancellationException::class)
    fun findFirstBlocking(includeInvisible: Boolean = false): ViewNode? {
        return traverseAllNodeBlocking(startNode, includeInvisible = includeInvisible).let {
            if (it == null && rootCompat) {
                traverseAllNodeBlocking(
                    ViewNode.activeWinNode(),
                    includeInvisible = includeInvisible
                )
            } else it
        }
    }

    //[findAll]
    suspend fun find(includeInvisible: Boolean = false) = findAll(includeInvisible)

    @Throws(ViewNodeNotFoundException::class)
    suspend fun require(
        waitMillis: Long = FinderConfig.FINDER_WAIT_MILLIS,
        interval: Long = FinderConfig.FINDER_WAIT_INTERVAL,
        includeInvisible: Boolean = false
    ): ViewNode {
        return waitFor(waitMillis, interval, includeInvisible)
            ?: throw ViewNodeNotFoundException(this)
    }

    suspend fun exist(includeInvisible: Boolean = false): Boolean =
        findFirst(includeInvisible) != null

    fun existBlocking(includeInvisible: Boolean = false): Boolean =
        findFirstBlocking(includeInvisible) != null

    @Suppress("UNCHECKED_CAST")
    fun enableRootCompat(): T {
        rootCompat = true
        return this as T
    }

    /**
     *
     * @param includeInvisible Boolean 是否包含不可见元素
     * @return Array<ViewNode> 无结果则返回空
     */
    suspend fun findAll(includeInvisible: Boolean = false): Array<ViewNode> {
        val l = mutableListOf<ViewNode>()
        traverseAllNode(startNode, l, includeInvisible)
        if (l.isEmpty() && rootCompat) {
            traverseAllNode(ViewNode.activeWinNode(), l, includeInvisible)
        }
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
        includeInvisible: Boolean = false, depth: Int = 0,
        nodeSet: MutableSet<AcsNode> = mutableSetOf()
    ): ViewNode? {
        ensureActive()
        node ?: return null
        if (node.node in nodeSet) return null
        nodeSet.add(node.node)
        node.children.forEach { childNode ->
            ensureActive()
            if (childNode == null) {
                return@forEach
            }
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

    private fun traverseAllNodeBlocking(
        node: ViewNode?, list: MutableList<ViewNode>? = null,
        includeInvisible: Boolean = false, depth: Int = 0,
        nodeSet: MutableSet<AcsNode> = mutableSetOf()
    ): ViewNode? {
        ensureNotInterrupt()
        node ?: return null
        if (node.node in nodeSet) return null
        nodeSet.add(node.node)
        node.children.forEach { childNode ->
            ensureNotInterrupt()
            if (childNode == null) {
                return@forEach
            }
            if (!includeInvisible && !childNode.isVisibleToUser) {
                return@forEach
            }
            if (findCondition(childNode.node)) {
                if (list != null) {
                    list.add(childNode)
                } else return childNode
            }
            val r = traverseAllNodeBlocking(
                childNode,
                list, includeInvisible, depth + 1, nodeSet
            )
            if (list == null && r != null) {
                return r
            }
        }
        return null
    }

    /**
     * 等待消失  常用于加载View的消失
     * @return Boolean false 超时 true 消失
     */
    suspend fun waitHide(waitMs: Int, interval: Long = 50L): Boolean {
        return whileWaitTime(waitMs.toLong(), interval) {
            if (findFirst() != null) {
                null
            }// 显示，继续等待
            else {
                true
            } // 消失
        } ?: false
    }

    suspend fun await() = waitFor()

    suspend fun await(l: Long): ViewNode? = waitFor(l)

    /**
     * 查找条件
     */
    abstract fun findCondition(node: AcsNode): Boolean

    abstract fun finderInfo(): String
}
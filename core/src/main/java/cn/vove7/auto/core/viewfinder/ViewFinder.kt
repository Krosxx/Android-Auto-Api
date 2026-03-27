package cn.vove7.auto.core.viewfinder

import cn.vove7.auto.core.utils.ViewNodeNotFoundException
import cn.vove7.auto.core.utils.ensureActive
import cn.vove7.auto.core.utils.ensureNotInterrupt
import cn.vove7.auto.core.utils.whileWaitTime
import cn.vove7.auto.core.viewnode.ViewNode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min

/**
 * 查找符合条件的AccessibilityNodeInfo
 * @param node 开始节点
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class ViewFinder<T : ViewFinder<T>>(
    val node: ViewNode? = null
) {
    @Suppress("PropertyName")
    val DEBUG = FinderConfig.DEBUG_LOG

    val startNode: ViewNode get() = node ?: ViewNode.getRoot()

    // startNode 为 ViewNode.getRoot() 时，SDK_INT >= LOLLIPOP 搜索失败时，
    // 尝试走 SDK_INT < LOLLIPOP ViewNode.activeWinNode() 节点
    private var rootCompat: Boolean = FinderConfig.FINDER_ROOT_COMPAT
        get() = field && this.node == null

    private var includeInvisible: Boolean = FinderConfig.FINDER_INCLUDE_INVISIBLE

    /**
     * 等待搜索，在指定时间内循环搜索（视图更新），超时返回null
     * 等待View出现 同步 耗时操作
     * 主动搜索
     * @param waitTime Long 时限
     */
    suspend fun waitFor(
        waitTime: Long = FinderConfig.FINDER_WAIT_MILLIS,
        interval: Long = FinderConfig.FINDER_WAIT_INTERVAL,
    ): ViewNode? {
        val wt = min(30000, max(0, waitTime))
        val beginTime = System.currentTimeMillis()
        val endTime = beginTime + wt
        do {
            val node = findFirst()
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
    suspend fun requireFirst(): ViewNode =
        findFirst() ?: throw ViewNodeNotFoundException(this)

    @Throws(ViewNodeNotFoundException::class)
    fun requireFirstBlocking(): ViewNode =
        findFirstBlocking() ?: throw ViewNodeNotFoundException(this)

    /**
     * 查找第一个
     * @return ViewNode?
     */
    suspend fun findFirst(): ViewNode? =
        traverseAllNode(startNode, includeInvisible = includeInvisible).let {
            if (it == null && rootCompat) {
                Timber.d("findFirst with rootCompat")
                traverseAllNode(ViewNode.activeWinNode(), includeInvisible = includeInvisible)
            } else it
        }

    @Throws(CancellationException::class)
    fun findFirstBlocking(): ViewNode? {
        return traverseAllNodeBlocking(startNode, includeInvisible = includeInvisible).let {
            if (it == null && rootCompat) {
                if (DEBUG) {
                    Timber.d("findFirst with rootCompat")
                }
                traverseAllNodeBlocking(
                    ViewNode.activeWinNode(),
                    includeInvisible = includeInvisible
                )
            } else it
        }
    }

    // [findAll]
    suspend inline fun find() = findAll()

    @Throws(ViewNodeNotFoundException::class)
    suspend fun require(
        waitMillis: Long = FinderConfig.FINDER_WAIT_MILLIS,
        interval: Long = FinderConfig.FINDER_WAIT_INTERVAL,
    ): ViewNode = waitFor(waitMillis, interval)
        ?: throw ViewNodeNotFoundException(this)

    suspend fun exist(): Boolean = findFirst() != null

    fun existBlocking(): Boolean = findFirstBlocking() != null

    fun enableRootCompat(): T {
        rootCompat = true
        @Suppress("UNCHECKED_CAST")
        return this as T
    }

    fun includeInvisible(ii: Boolean = true): T {
        includeInvisible = ii
        @Suppress("UNCHECKED_CAST")
        return this as T
    }

    /**
     * 查找全部符合条件的 Node
     * @return List<ViewNode> 无结果则返回空
     */
    suspend fun findAll(): List<ViewNode> {
        val l = mutableListOf<ViewNode>()
        traverseAllNode(startNode, includeInvisible, l)
        if (l.isEmpty() && rootCompat) {
            if (DEBUG) {
                Timber.d("findAll with rootCompat")
            }
            traverseAllNode(ViewNode.activeWinNode(), includeInvisible, l)
        }
        return l
    }

    fun findAllBlocking(): List<ViewNode> {
        val l = mutableListOf<ViewNode>()
        traverseAllNodeBlocking(startNode, l, includeInvisible)
        if (l.isEmpty() && rootCompat) {
            if (DEBUG) {
                Timber.d("findAll with rootCompat")
            }
            traverseAllNodeBlocking(ViewNode.activeWinNode(), l, includeInvisible)
        }
        return l
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
        node: ViewNode?,
        includeInvisible: Boolean = false,
        list: MutableList<ViewNode>? = null,
        depth: Int = 0,
        nodeSet: MutableSet<AcsNode> = mutableSetOf()
    ): ViewNode? {
        ensureActive()
        node ?: return null
        if (node.node in nodeSet) return null
        nodeSet.add(node.node)
        val interrupt = AtomicBoolean(false)
        node.children.forEach { childNode ->
            ensureActive()
            if (childNode == null) {
                return@forEach
            }
            if (!includeInvisible && !childNode.isVisibleToUser) {
                return@forEach
            }
            interrupt.set(false)
            val matched = findCondition(childNode.node, interrupt)
            if (matched) {
                if (list != null) {
                    list.add(childNode)
                } else return childNode
            }
            if (!matched && interrupt.get()) {
                // skip children search
                if (DEBUG) {
                    Timber.d("skip children search $childNode")
                }
            } else {
                val r = traverseAllNode(childNode, includeInvisible, list, depth + 1)
                if (list == null && r != null) {
                    return r
                }
            }
        }
        return null
    }

    private fun traverseAllNodeBlocking(
        node: ViewNode?, list: MutableList<ViewNode>? = null,
        includeInvisible: Boolean = false, depth: Int = 0,
        nodeSet: MutableSet<Int> = mutableSetOf()
    ): ViewNode? {
        ensureNotInterrupt()
        node ?: return null
        if (node.hashCode() in nodeSet) return null
        nodeSet.add(node.hashCode())
        val interrupt = AtomicBoolean(false)
        node.children.forEach { childNode ->
            ensureNotInterrupt()
            if (childNode == null) {
                return@forEach
            }
            if (!includeInvisible && !childNode.isVisibleToUser) {
                return@forEach
            }
            interrupt.set(false)
            val matched = findCondition(childNode.node, interrupt)
            if (matched) {
                if (list != null) {
                    list.add(childNode)
                } else return childNode
            }
            if (!matched && interrupt.get()) {
                // skip children search
                if (DEBUG) {
                    Timber.d("skip children search $childNode")
                }
            } else {
                val r = traverseAllNodeBlocking(
                    childNode,
                    list, includeInvisible, depth + 1, nodeSet
                )
                if (list == null && r != null) {
                    return r
                }
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

    // [waitFor]
    suspend fun await(
        waitTime: Long = FinderConfig.FINDER_WAIT_MILLIS,
        interval: Long = FinderConfig.FINDER_WAIT_INTERVAL,
    ): ViewNode? = waitFor(waitTime, interval)

    /**
     * 查找条件
     */
    abstract fun findCondition(node: AcsNode, interrupt: AtomicBoolean): Boolean

    abstract fun finderInfo(): String
}
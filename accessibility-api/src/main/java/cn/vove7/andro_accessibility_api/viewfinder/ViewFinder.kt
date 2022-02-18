package cn.vove7.andro_accessibility_api.viewfinder

import android.view.accessibility.AccessibilityNodeInfo
import cn.vove7.andro_accessibility_api.AccessibilityApi
import cn.vove7.andro_accessibility_api.utils.NeedBaseAccessibilityException
import cn.vove7.andro_accessibility_api.utils.ViewNodeNotFoundException
import cn.vove7.andro_accessibility_api.utils.whileWaitTime
import cn.vove7.andro_accessibility_api.viewfinder.FinderBuilderWithOperation.Companion.WAIT_MILLIS
import cn.vove7.andro_accessibility_api.viewnode.ViewNode
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * 查找符合条件的AccessibilityNodeInfo
 * @param node 开始节点
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class ViewFinder<T : ViewFinder<T>>(
    val node: ViewNode? = null
) {
    private var coroutineCtx: CoroutineContext? = null

    /**
     * 协程支持
     */
    suspend fun attachCoroutine(): T {
        coroutineCtx = coroutineContext
        @Suppress("UNCHECKED_CAST")
        return this as T
    }

    private fun ensureActive() {
        coroutineCtx?.ensureActive()
    }

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
        fun findByDepths(depths: IntArray, node: ViewNode): ViewNode? {
            var p: ViewNode? = node
            depths.forEach {
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

        fun findByDepths(vararg depths: Int) = findByDepths(depths, ROOT_NODE)
    }

    val startNode: ViewNode
        get() = node ?: ROOT_NODE

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
            ensureActive()
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
    fun findFirst(includeInvisible: Boolean = false): ViewNode? {
        //不可见
        return traverseAllNode(startNode, includeInvisible = includeInvisible)
    }

    /**
     * 使用深度搜索
     * @param depths Array<Int>
     * @return ViewNode?
     */
    fun findByDepths(vararg depths: Int): ViewNode? {
        return Companion.findByDepths(depths, startNode)
    }

    //[findAll]
    fun find(includeInvisible: Boolean = false) = findAll(includeInvisible)

    @Throws(ViewNodeNotFoundException::class)
    fun require(waitMillis: Long = WAIT_MILLIS): ViewNode {
        return waitFor(waitMillis) ?: throw ViewNodeNotFoundException(this)
    }

    fun exist(): Boolean = findFirst() != null

    /**
     *
     * @param includeInvisible Boolean 是否包含不可见元素
     * @return Array<ViewNode> 无结果则返回空
     */
    fun findAll(includeInvisible: Boolean = false): Array<ViewNode> {
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
    private fun traverseAllNode(
        node: ViewNode?, list: MutableList<ViewNode>? = null,
        includeInvisible: Boolean = false, depth: Int = 0
    ): ViewNode? {
        ensureActive()
        node ?: return null
        if (depth > MAX_DEPTH) {//防止出现无限递归（eg:QQ浏览器首页）
            return null
        }
        node.children.forEach { childNode ->
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
    fun waitHide(): Boolean {
        return waitHide(10000)
    }

    /**
     * 等待消失  常用于加载View的消失
     * @param waitMs max 60s
     * @return Boolean false 超时 true 消失
     */
    fun waitHide(waitMs: Int): Boolean {
        return whileWaitTime(waitMs.toLong()) {
            ensureActive()
            if (findFirst() != null) {
                null
            }//显示，继续等待
            else {
                true
            } //消失
        } ?: false
    }

    fun await(): ViewNode? {
        return waitFor()
    }

    fun await(l: Long): ViewNode? {
        return waitFor(l)
    }

    /**
     * 查找条件
     */
    abstract fun findCondition(node: AccessibilityNodeInfo): Boolean

}
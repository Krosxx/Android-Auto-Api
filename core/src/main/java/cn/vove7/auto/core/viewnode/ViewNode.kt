package cn.vove7.auto.core.viewnode

import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import cn.vove7.auto.core.AutoApi
import cn.vove7.auto.core.utils.ScreenAdapter
import cn.vove7.auto.core.utils.ViewChildList
import cn.vove7.auto.core.viewfinder.AcsNode
import cn.vove7.auto.core.viewfinder.SmartFinder
import kotlinx.coroutines.runBlocking
import java.lang.Thread.sleep

/**
 * 视图节点
 * @property node 无障碍视图节点
 */
@Suppress("MemberVisibilityCanBePrivate")
class ViewNode : ViewOperation, Comparable<ViewNode> {
    val node: AcsNode

    constructor(node: AccessibilityNodeInfo) {
        this.node = AcsNode.wrap(node)
    }

    constructor(node: AcsNode) {
        this.node = node
    }


    /**
     * 文本相似度
     */
    var similarityText: Float = 0f

    private var childrenCache: ViewChildList? = null

    private var buildWithChildren = false

    companion object {
        var TRY_OP_NUM = 10

        private const val ROOT_TAG = "ViewNodeRoot"

        fun rootNodesOfAllWindows(): ViewChildList =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ViewChildList().also { list ->
                    AutoApi.windows()?.sortedByDescending {
                        if (it.isActive) Int.MAX_VALUE else it.layer
                    }?.forEach { win ->
                        list.add(
                            win.root?.let { r -> ViewNode(r.also(AccessibilityNodeInfo::refresh)) }
                        )
                    } ?: list.add(activeWinNode())
                }
            } else {
                ViewChildList().also { list ->
                    list.add(activeWinNode())
                }
            }

        /**
         * 第一层为 windows
         */
        fun getRoot(): ViewNode {
            return withChildren(rootNodesOfAllWindows())
        }

        fun activeWinNode(): ViewNode? {
            return AutoApi.rootInActiveWindow()?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    ViewNode(it.also(AccessibilityNodeInfo::refresh))
                } else {
                    ViewNode(it)
                }
            }
        }

        fun withChildren(cs: ViewChildList): ViewNode {
            val root = AccessibilityNodeInfo.obtain()
            root.className = "$ROOT_TAG[Win Size: ${cs.size}]"
            return ViewNode(root).apply {
                buildWithChildren = true
                childrenCache = cs
            }
        }
    }

    override val id: String
        get() = node.viewIdResourceName

    override val className get() = node.className?.toString()

    override val packageName: String? get() = node.packageName?.toString()

    override val boundsInParent: Rect
        get() {
            val out = Rect()
            @Suppress("DEPRECATION")
            node.getBoundsInParent(out)
            return out
        }


    override val bounds: Rect
        get() {
            val out = Rect()
            node.getBoundsInScreen(out)
            return out
        }

    override val parent: ViewNode?
        get() = if (buildWithChildren) null else node.parent?.let { ViewNode(it) }

    override val previousSibling: ViewNode?
        get() = parent?.children?.let { parChildren ->
            parChildren.getOrNull(parChildren.indexOf(this) - 1)
        }

    override val nextSibling: ViewNode?
        get() = parent?.children?.let { parChildren ->
            parChildren.getOrNull(parChildren.indexOf(this) + 1)
        }

    override fun tryClick(): Boolean {
        return tryOp(AccessibilityNodeInfo.ACTION_CLICK)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun globalClick(): Boolean {
        // 获得中心点
        val relp = ScreenAdapter.getRelPoint(getCenterPoint())
        return runBlocking {
            cn.vove7.auto.core.api.click(relp.x, relp.y)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override suspend fun globalLongClick(): Boolean {
        val relp = ScreenAdapter.getRelPoint(getCenterPoint())
        return cn.vove7.auto.core.api.longClick(relp.x, relp.y)
    }

    /**
     * 尝试操作次数
     * 点击，长按，选择
     * 尝试点击父级
     */
    private fun tryOp(action: Int): Boolean {
        var p = node
        var i = 0
        while (i < TRY_OP_NUM && !p.performAction(action)) {
            if (p.parent?.also { p = it } == null) {
                return false
            }
            i++
        }
        return i != TRY_OP_NUM
    }

    fun clearChildrenCache() {
        if (!buildWithChildren) {
            childrenCache = null
        }
    }

    override val children: ViewChildList
        get() {
            val cc = childrenCache
            if (cc != null) {
                return cc
            }
            return ViewChildList(this).also {
                childrenCache = it
            }
        }

    override val childCount: Int
        get() = if (buildWithChildren) childrenCache?.size ?: 0 else node.childCount

    override fun childAt(i: Int): ViewNode? {
        // IllegalStateException: Cannot perform this action on a not sealed instance.
        if (buildWithChildren) {
            return childrenCache?.get(i)
        }
        val cn = try {
            node.getChild(i)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            null
        }
        return cn?.let { ViewNode(it) }
    }

    override fun click(): Boolean = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)

    override fun doubleClick(): Boolean {
        return if (tryClick()) {
            sleep((ViewConfiguration.getDoubleTapTimeout() + 50).toLong())
            tryClick()
        } else false
    }

    override fun tryLongClick(): Boolean {
        return tryOp(AccessibilityNodeInfo.ACTION_LONG_CLICK)
    }

    override fun longClick(): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
    }

    override fun select(): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_SELECT)
    }

    override fun setSelection(start: Int, end: Int): Boolean {
        val args = Bundle()
        args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, start)
        args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, end)
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)
    }

    override fun clearSelection(): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_CLEAR_SELECTION)
    }

    override fun trySelect(): Boolean {
        return tryOp(AccessibilityNodeInfo.ACTION_SELECT)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun scrollUp() =
        node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP.id)

    @RequiresApi(Build.VERSION_CODES.M)
    override fun scrollDown(): Boolean {
        return node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN.id)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun scrollForward(): Boolean {
        return node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD.id)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun scrollBackward(): Boolean {
        return node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD.id)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun scrollLeft(): Boolean {
        return node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_LEFT.id)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun scrollRight(): Boolean {
        return node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT.id)
    }

    override var text: CharSequence?
        get() {
            refresh()
            return node.text
        }
        set(v) {
            val arg = Bundle()
            arg.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, v)
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arg)
        }

    override var hintText: CharSequence?
        get() = node.hintText
        set(value) {
            node.hintText = value
        }

    override var progress: Float
        get() = node.rangeInfo.current
        @RequiresApi(Build.VERSION_CODES.N)
        set(value) {
            node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id,
                Bundle().also {
                    it.putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, value)
                })
        }

    override val rangeInfo: AccessibilityNodeInfoCompat.RangeInfoCompat
        get() = node.rangeInfo

    override fun desc(): String? {
        return node.contentDescription?.toString()
    }

    override fun appendText(s: CharSequence) {
        text = buildString {
            append(text)
            append(s)
        }
    }

    override fun trySetText(text: CharSequence): Boolean {
        val arg = Bundle()
        arg.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        var p = node
        var i = 0
        while (i < TRY_OP_NUM && !p.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arg)) {
            p = node.parent
            i++
        }
        val b = i != TRY_OP_NUM
        return b
    }

    override fun getCenterPoint(): Point {
        val rect = bounds
        val x = (rect.left + rect.right) / 2
        val y = (rect.top + rect.bottom) / 2
        return Point(x, y)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun swipeOffset(dx: Int, dy: Int, delay: Int): Boolean {
        val c = ScreenAdapter.getRelPoint(getCenterPoint())
        return runBlocking {
            cn.vove7.auto.core.api.swipe(c.x, c.y, c.x + dx, c.y + dy, delay)
        }
    }

    override fun focus(): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
    }

    override fun clearFocus(): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS)
    }

    override fun compareTo(other: ViewNode): Int {
        return ((other.similarityText - similarityText) * 100).toInt()
    }

    override fun toString(): String {
        return nodeSummary(node)
    }

    fun sortOutInfo(): ViewInfo {
        return ViewInfo(
            text,
            node.contentDescription,
            className,
            boundsInParent,
            bounds
//                node.isClickable,
//                null,
//                node.canOpenPopup()
        )
    }

    private fun nodeSummary(node: AcsNode): String {
        val id = node.viewIdResourceName
        val desc = node.contentDescription
        return buildString {
            append("{ class: ").append(className)
            if (id != null) append(", id: ").append(id.substring(id.lastIndexOf('/') + 1))
            if (node.text != null) append(", text: ${node.text}")
            if (hintText != null) append(", hintText: $hintText")
            if (desc != null) append(", desc: $desc")
            append(", bounds: $bounds, childCount: $childCount")
            if (node.isEditable) append(", Editable")
            if (node.isClickable) append(", Clickable")
            if (node.isSelected) append(", Selected")
            if (!node.isVisibleToUser) append(", InVisible")
            if (!node.isEnabled) append(", Disabled")
            if (node.isPassword) append(", Password")
            if (node.isChecked) append(", Checked")
            if (node.isFocused) append(", Focused")
            if (node.isScrollable) append(", Scrollable")
            if (node.isDismissable) append(", Dismissable")
            append(" }")
        }
    }

    /**
     * 从该节点搜索
     * @return SmartFinder
     */
    override fun finder() = SmartFinder(this)

    override var isVisibleToUser: Boolean
        get() {
            return if (className?.startsWith(ROOT_TAG) == true) true
            else node.isVisibleToUser
        }
        set(value) {
            node.isVisibleToUser = value
        }

    override fun isClickable(): Boolean {
        return node.isClickable
    }

    override fun refresh(): Boolean {
        if (buildWithChildren) {
            childrenCache = rootNodesOfAllWindows()
            return true
        }
        return node.refresh()
    }

    override val actionList: List<AccessibilityActionCompat>
        get() = node.actionList

    @RequiresApi(Build.VERSION_CODES.R)
    override fun sendImeAction(): Boolean {
        return node.performAction(AccessibilityActionCompat.ACTION_IME_ENTER.id)
    }

    override fun equals(other: Any?): Boolean {
        return if (other is ViewNode) {
            node == other.node
        } else false
    }

    override fun hashCode() = node.hashCode()

}

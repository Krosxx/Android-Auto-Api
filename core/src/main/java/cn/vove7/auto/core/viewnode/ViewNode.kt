package cn.vove7.auto.core.viewnode

import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import cn.vove7.auto.core.AutoApi
import cn.vove7.auto.core.utils.ScreenAdapter
import cn.vove7.auto.core.utils.ViewChildList
import cn.vove7.auto.core.utils.ViewNodeNotFoundException
import cn.vove7.auto.core.utils.ensureActive
import cn.vove7.auto.core.viewfinder.AcsNode
import cn.vove7.auto.core.viewfinder.FinderConfig
import cn.vove7.auto.core.viewfinder.SmartFinder
import java.lang.Thread.sleep

/**
 * 视图节点
 * @property node 无障碍视图节点
 */
@Suppress("MemberVisibilityCanBePrivate")
class ViewNode : ViewOperation {
    val node: AcsNode

    constructor(node: AccessibilityNodeInfo) {
        this.node = AcsNode.wrap(node)
    }

    constructor(node: AcsNode) {
        this.node = node
    }

    private var childrenCache: ViewChildList? = null

    private var buildWithChildren = false

    companion object {

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

        // 使用 ViewNode.activeWinNode()
        suspend fun findByDepths(vararg depths: Int) = activeWinNode()?.findByDepths(*depths)

        suspend fun requireByDepths(vararg depths: Int): ViewNode {
            val root = activeWinNode()
            return root?.findByDepths(*depths)
                ?: throw ViewNodeNotFoundException(
                    "can not find view by depths: ${depths.contentToString()}" +
                            ", startNode: root($root)"
                )
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
    override suspend fun globalClick(): Boolean {
        // 获得中心点
        val relp = ScreenAdapter.getRelPoint(getCenterPoint())
        return cn.vove7.auto.core.api.click(relp.x, relp.y)
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
        while (i < FinderConfig.TRY_OP_CNT) {
            if (p.performAction(action)) return true
            if (p.parent?.also { p = it } == null) return false
            i++
        }
        return false
    }

    fun clearChildrenCache() {
        if (!buildWithChildren) {
            childrenCache = null
        }
    }

    override val children: ViewChildList
        get() {
            val cc = childrenCache
            if (cc != null) return cc
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
        return node.performAction(
            AccessibilityNodeInfo.ACTION_SET_SELECTION, bundleOf(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT to start,
                AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT to end
            )
        )
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
        get() = node.text
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
            node.performAction(
                AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id,
                bundleOf(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE to value)
            )
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
        val arg = bundleOf(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE to text)
        var p = node
        var i = 0
        while (i < FinderConfig.TRY_OP_CNT && !p.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT, arg
            )
        ) {
            p = node.parent
            i++
        }
        val b = i != FinderConfig.TRY_OP_CNT
        return b
    }

    override fun getCenterPoint(): Point {
        val rect = bounds
        val x = (rect.left + rect.right) / 2
        val y = (rect.top + rect.bottom) / 2
        return Point(x, y)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override suspend fun swipeOffset(dx: Int, dy: Int, delay: Int): Boolean {
        val c = ScreenAdapter.getRelPoint(getCenterPoint())
        return cn.vove7.auto.core.api.swipe(c.x, c.y, c.x + dx, c.y + dy, delay)
    }

    override fun focus(): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
    }

    override fun clearFocus(): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS)
    }

    override val isShowingHint: Boolean get() = node.isShowingHintText

    override fun toString(): String {
        return nodeSummary(node)
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
            append(", bounds: $bounds")
            bounds.also {
                append(", b_width: ")
                append(it.width())
                append(", b_height: ")
                append(it.height())
            }
            boundsInParent.also {
                append(", p_width: ")
                append(it.height())
                append(", p_height: ")
                append(it.height())
            }
            append(", childCount: $childCount")
            if (node.isEditable) append(", Editable")
            if (node.isShowingHintText) append(", ShowingHint")
            if (node.isImportantForAccessibility) append(", Important")
            if (node.isClickable) append(", Clickable")
            if (node.isContextClickable) append(", ContextClickable")
            if (node.isLongClickable) append(", LongClickable")
            if (node.isSelected) append(", Selected")
            if (!node.isVisibleToUser) append(", InVisible")
            if (!node.isEnabled) append(", Disabled")
            if (node.isPassword) append(", Password")
            if (node.isCheckable) append(", Checkable")
            if (node.isChecked) append(", Checked")
            if (node.isFocusable) append(", Focusable")
            if (node.isScreenReaderFocusable) append(", ReaderFocusable")
            if (node.isFocused) append(", Focused")
            if (node.isScrollable) append(", Scrollable")
            if (node.isDismissable) append(", Dismissable")
            if (node.isAccessibilityFocused) append(", AccessibilityFocused")
            if (node.canOpenPopup()) append(", CanOpenPopup")
            append(", hash: 0x")
            append(Integer.toHexString(node.hashCode()))
            append(" }")
        }
    }

    /**
     * 从该节点搜索 SmartFinder
     * @return SmartFinder
     */
    override fun finder() = SmartFinder(this)

    /**
     * 使用深度搜索
     * @param depths Array<Int>
     * @return ViewNode?
     */
    override suspend fun findByDepths(vararg depths: Int): ViewNode? {
        var p: ViewNode? = this
        depths.forEach {
            ensureActive()
            try {
                p = p?.childAt(it)
            } catch (e: IndexOutOfBoundsException) {
                return null
            }
            if (p == null) {
                return null
            }
        }
        return p
    }

    override var isVisibleToUser: Boolean
        get() {
            return if (className?.startsWith(ROOT_TAG) == true) true
            else node.isVisibleToUser
        }
        set(value) {
            node.isVisibleToUser = value
        }

    override fun isClickable() = node.isClickable

    override fun refresh(): Boolean {
        childrenCache = null
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

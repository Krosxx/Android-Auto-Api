package cn.vove7.andro_accessibility_api.viewnode

import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import cn.vove7.andro_accessibility_api.AccessibilityApi
import cn.vove7.andro_accessibility_api.api.swipe
import cn.vove7.andro_accessibility_api.utils.ScreenAdapter
import cn.vove7.andro_accessibility_api.viewfinder.SmartFinder
import java.lang.Thread.sleep

/**
 * 视图节点
 * @property node 无障碍视图节点
 */
@Suppress("MemberVisibilityCanBePrivate")
class ViewNode(
    val node: AccessibilityNodeInfo
) : ViewOperation, Comparable<ViewNode> {

    val nodeWrapper = AccessibilityNodeInfoCompat.wrap(node)

    /**
     * 文本相似度
     */
    var similarityText: Float = 0f

    private var childrenCache: Array<ViewNode>? = null

    private var buildWithChildren = false

    companion object {
        var TRY_OP_NUM = 10

        private const val ROOT_TAG = "ViewNodeRoot"

        private fun rootNodesOfAllWindows() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AccessibilityApi.requireBase.windows?.mapNotNull {
                    it.root?.let { r -> ViewNode(r.also(AccessibilityNodeInfo::refresh)) }
                } ?: emptyList()
            } else {
                AccessibilityApi.requireBase.activeWinNode?.let { listOf(it) } ?: emptyList()
            }

        /**
         * 第一层为 windows
         */
        fun getRoot(): ViewNode {
            return withChildren(rootNodesOfAllWindows())
        }

        fun activeWinNode(): ViewNode? {
            return AccessibilityApi.requireBase.rootInActiveWindow?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    ViewNode(it.also(AccessibilityNodeInfo::refresh))
                } else {
                    ViewNode(it)
                }
            }
        }

        fun withChildren(cs: List<ViewNode>): ViewNode {
            val root = AccessibilityNodeInfo.obtain()
            root.className = "${ROOT_TAG}[Win Size: ${cs.size}]"
            return ViewNode(root).apply {
                buildWithChildren = true
                childrenCache = cs.toTypedArray()
            }
        }
    }

    override val id: String
        get() = nodeWrapper.viewIdResourceName

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

    override fun tryClick(): Boolean {
        return tryOp(AccessibilityNodeInfo.ACTION_CLICK)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun globalClick(): Boolean {
        //获得中心点
        val relp = ScreenAdapter.getRelPoint(getCenterPoint())
        return cn.vove7.andro_accessibility_api.api.click(relp.x, relp.y)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun globalLongClick(): Boolean {
        val relp = ScreenAdapter.getRelPoint(getCenterPoint())
        return cn.vove7.andro_accessibility_api.api.longClick(relp.x, relp.y)
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

    fun clearChildCache() {
        if (!buildWithChildren) {
            childrenCache = null
        }
    }

    override val children: Array<ViewNode>
        get() {
            val cc = childrenCache
            if (cc != null) {
                return cc
            }
            return (0 until node.childCount).mapNotNull { i ->
                node.getChild(i)?.let { ViewNode(it) }
            }.toTypedArray().also {
                childrenCache = it
            }
        }

    override val childCount: Int
        get() = if (buildWithChildren) childrenCache?.size ?: 0 else node.childCount

    override fun childAt(i: Int): ViewNode? {
        //IllegalStateException: Cannot perform this action on a not sealed instance.
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

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
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
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        set(v) {
            val arg = Bundle()
            arg.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, v)
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arg)
        }

    override var hintText: CharSequence?
        @RequiresApi(Build.VERSION_CODES.O)
        get() = node.hintText
        @RequiresApi(Build.VERSION_CODES.O)
        set(value) {
            node.hintText = value
        }

    override fun desc(): String? {
        return node.contentDescription?.toString()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun appendText(s: CharSequence) {
        text = buildString {
            append(text)
            append(s)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
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
    override fun swipe(dx: Int, dy: Int, delay: Int): Boolean {
        val c = ScreenAdapter.getRelPoint(getCenterPoint())
        return swipe(c.x, c.y, c.x + dx, c.y + dy, delay)
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

    override val className get() = node.className?.toString()

    private fun nodeSummary(node: AccessibilityNodeInfo): String {
        val id = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            node.viewIdResourceName
        } else {
            null
        }
        val desc = node.contentDescription
        return buildString {
            append("{ class: ").append(className)
            if (id != null) append(", id: ").append(id.substring(id.lastIndexOf('/') + 1))
            if (node.text != null) append(", text: ${node.text}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hintText != null) append(", hintText: $hintText")
            if (desc != null) append(", desc: $desc")
            append(", bounds: $bounds, childCount: $childCount")
            if (node.isClickable) append(", Clickable")
            if (node.isSelected) append(", Selected")
            if (!node.isVisibleToUser) append(", InVisibleToUser")
            if (!node.isEnabled) append(", Disabled")
            if (node.isPassword) append(", Password")
            if (node.isChecked) append(", Checked")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && node.isDismissable) append(", Dismissable")
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
            childrenCache = rootNodesOfAllWindows().toTypedArray()
            return true
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            node.refresh()
        } else {
            false
        }
    }

    override val actionList: List<AccessibilityNodeInfo.AccessibilityAction>
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        get() = node.actionList

    @RequiresApi(Build.VERSION_CODES.R)
    override fun sendImeAction(): Boolean {
        return node.performAction(android.R.id.accessibilityActionImeEnter)
    }
}

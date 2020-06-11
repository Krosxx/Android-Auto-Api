package cn.vove7.andro_accessibility_api.viewnode

import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import cn.vove7.andro_accessibility_api.api.swipe
import cn.vove7.andro_accessibility_api.utils.ScreenAdapter
import cn.vove7.andro_accessibility_api.viewfinder.ViewFindBuilder
import java.lang.Thread.sleep

/**
 * 视图节点
 * @property node 无障碍视图节点
 */
@Suppress("MemberVisibilityCanBePrivate")
class ViewNode(val node: AccessibilityNodeInfo) : ViewOperation, Comparable<ViewNode> {

    /**
     * 文本相似度
     */
    var similarityText: Float = 0f

    private var childrenCache: Array<ViewNode>? = null

    companion object {
        var TRY_OP_NUM = 10

        private const val ROOT_TAG = "ViewNodeRoot"

        fun withChildren(cs: List<ViewNode>): ViewNode {
            val root = AccessibilityNodeInfo.obtain()
            root.className = "${ROOT_TAG}[Win Size: ${cs.size}]"
            return ViewNode(root).also {
                it.childrenCache = cs.toTypedArray()
            }
        }
    }

    override val id: String
        get() = node.viewIdResourceName

    override val boundsInParent: Rect
        get() {
            val out = Rect()
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
        get() {
            val it = node.parent
            return if (it != null) {
                ViewNode(it)
            } else null
        }

    override fun tryClick(): Boolean {
        return tryOp(AccessibilityNodeInfo.ACTION_CLICK)
    }

    override fun globalClick(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //获得中心点
            val relp = ScreenAdapter.getRelPoint(getCenterPoint())
            return cn.vove7.andro_accessibility_api.api.click(relp.x, relp.y)
        }
        return false
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

    override val children: Array<ViewNode>
        get() {
            if (childrenCache != null) {//10s有效期
                return childrenCache ?: emptyArray()
            }
            return (0 until node.childCount).mapNotNull { i ->
                node.getChild(i)?.let { ViewNode(it) }
            }.toTypedArray().also {
                childrenCache = it
            }
        }

    override fun getChildCount(): Int = node.childCount


    override fun childAt(i: Int): ViewNode? {
        val cn = node.getChild(i)
        if (cn != null) {
            return ViewNode(cn)
        }
        return null
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

    override fun scrollUp(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP.id)
        } else {
            false
        }
    }

    override fun scrollDown(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN.id)
        } else {
            false
        }
    }

    override fun scrollForward(): Boolean {
        return node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD.id)
    }

    override fun scrollBackward(): Boolean {
        return node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD.id)
    }

    override fun scrollLeft(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_LEFT.id)
        } else {
            false
        }
    }

    override fun scrollRight(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT.id)
        } else {
            false
        }
    }

    override var text: CharSequence?
        get() {
            node.refresh()
            return node.text
        }
        set(v) {
            val arg = Bundle()
            arg.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, v)
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arg)
        }

    override var hintText: CharSequence?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            node.hintText
        } else {
            null
        }
        @RequiresApi(Build.VERSION_CODES.O)
        set(value) {
            node.hintText = value
        }

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
            classType,
            boundsInParent,
            bounds
//                node.isClickable,
//                null,
//                node.canOpenPopup()
        )
    }

    val className get() = classType

    val classType: String?
        get() = node.className.let { it.substring(it.lastIndexOf('.') + 1) }


    private fun nodeSummary(node: AccessibilityNodeInfo): String {
        val id = node.viewIdResourceName
        val desc = node.contentDescription

        return "{ class: " + classType +
                (if (id == null) "" else ", id: " + id.substring(id.lastIndexOf('/') + 1)) +
                (if (node.text == null) "" else ", text: ${node.text}") +
                (if (hintText == null) "" else ", hintText: $hintText") +
                (if (desc == null) "" else ", desc: $desc") +
                (", bounds: $bounds" + ", childCount: ${getChildCount()}") +
                (if (node.isClickable) ", Clickable" else "") +
                (if (node.isSelected) ", Selected" else "") +
                (if (!node.isVisibleToUser) ", InVisibleToUser" else "") +
                (if (!node.isEnabled) ", Disabled" else "") +
                (if (node.isPassword) ", Password" else "") +
                (if (node.isChecked) ", Checked" else "") +
                (if (node.isDismissable) ", Dismissable" else "") +
                " }"
    }

    /**
     * 从该节点搜索
     * @return ViewFindBuilder
     */
    override fun finder(): ViewFindBuilder {
        return ViewFindBuilder(this)
    }

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
        if (className?.startsWith(ROOT_TAG) == true) {
            childrenCache = null
        }
        return node.refresh()
    }

    override val actionList: List<AccessibilityNodeInfo.AccessibilityAction>
        get() = node.actionList

}

package cn.vove7.andro_accessibility_api.viewfinder

import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import cn.vove7.andro_accessibility_api.utils.ViewNodeNotFoundException
import cn.vove7.andro_accessibility_api.viewnode.ViewNode
import cn.vove7.andro_accessibility_api.viewnode.ViewOperation

/**
 * # FindBuilderWithOperation
 *
 * @author 17719
 * 2018/8/10
 */

interface FindBuilderWithOperation : ViewOperation {

    companion object {
        var WAIT_MILLIS = 2000L
    }

    var finder: ViewFinder<*>

    /**
     * 找到第一个
     * @return ViewNode
     */
    fun findFirst(): ViewNode? = finder.findFirst()

    fun waitFor(): ViewNode? = waitFor(30000L)

    /**
     *
     * @param waitMillis 时限
     * @return ViewNode which is returned until show in screen
     */
    fun waitFor(waitMillis: Long): ViewNode? = finder.waitFor(waitMillis)

    @Throws(ViewNodeNotFoundException::class)
    fun require(waitMillis: Long = WAIT_MILLIS): ViewNode {
        return waitFor(waitMillis) ?: throw ViewNodeNotFoundException(finder)
    }

    fun exist(): Boolean = findFirst() != null

    /**
     *
     * @return list
     */
    fun find(): Array<ViewNode> {
        return finder.findAll()
    }

    private val node: ViewNode
        get() = require()

    override val id get() = node.id

    override fun tryClick(): Boolean = node.tryClick()

    override fun click(): Boolean = node.tryClick()

    override fun globalClick(): Boolean = node.globalClick()

    override fun longClick(): Boolean = node.longClick()

    override fun doubleClick(): Boolean {
        return node.doubleClick()
    }

    override fun tryLongClick(): Boolean {
        return node.tryLongClick()
    }

    override fun getCenterPoint(): Point? {
        return node.getCenterPoint()
    }

    override fun select(): Boolean {
        return node.select()
    }

    override var hintText: CharSequence?
        get() = node.hintText
        @RequiresApi(Build.VERSION_CODES.O)
        set(value) {
            node.hintText = value
        }

    override fun setSelection(start: Int, end: Int): Boolean = node.setSelection(start, end)

    override fun clearSelection(): Boolean = node.clearSelection()

    override fun trySelect(): Boolean {
        return node.trySelect()
    }

    override fun scrollUp(): Boolean {
        return node.scrollUp()
    }

    override fun scrollDown(): Boolean {
        return node.scrollDown()
    }

    override fun appendText(s: CharSequence) {
        node.appendText(s)
    }

    override fun desc(): String? {
        return node.desc()
    }


    override fun trySetText(text: CharSequence): Boolean {
        return node.trySetText(text)
    }

    override var text: CharSequence?
        get() = node.text
        set(v) {
            node.text = v
        }


    override fun focus(): Boolean {
        return node.focus()
    }

    override fun clearFocus(): Boolean {
        return node.clearFocus()
    }

    override fun scrollForward(): Boolean {
        return node.scrollForward()
    }

    override fun scrollBackward(): Boolean {
        return node.scrollBackward()
    }

    override fun scrollLeft(): Boolean {
        return node.scrollLeft()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun swipe(dx: Int, dy: Int, delay: Int): Boolean {
        return node.swipe(dx, dy, delay)
    }

    override fun scrollRight(): Boolean {
        return node.scrollRight()
    }

    override val children: Array<ViewNode>
        get() = node.children

    override fun childAt(i: Int): ViewNode? {
        return node.childAt(i)
    }

    override val bounds: Rect?
        get() = node.bounds

    override val boundsInParent: Rect?
        get() = node.boundsInParent

    override val parent: ViewNode?
        get() = node.parent


    override fun getChildCount(): Int? {
        return node.getChildCount()
    }

    override fun isClickable(): Boolean {
        return node.isClickable()
    }

    override fun finder(): ViewFindBuilder {
        return node.finder()
    }

    override var isVisibleToUser: Boolean
        get() = node.isVisibleToUser
        set(value) {
            node.isVisibleToUser = value
        }

    override fun refresh(): Boolean = node.refresh()

    override val actionList: List<AccessibilityNodeInfo.AccessibilityAction>
        get() = node.actionList

}

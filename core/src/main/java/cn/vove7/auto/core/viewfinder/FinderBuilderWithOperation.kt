package cn.vove7.auto.core.viewfinder

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import cn.vove7.auto.core.viewnode.ViewNode
import cn.vove7.auto.core.viewnode.ViewOperation
import kotlinx.coroutines.runBlocking

/**
 * # FindBuilderWithOperation
 *
 * @author Vove
 * 2018/8/10
 */

interface FinderBuilderWithOperation : ViewOperation {

    companion object {
        var WAIT_MILLIS = 2000L
    }

    val finder: ViewFinder<*>

    private val node get() = runBlocking { finder.require() }

    override val id get() = node.id
    override val className get() = node.className
    override val packageName: String? get() = node.packageName

    @RequiresApi(Build.VERSION_CODES.N)
    override suspend fun globalLongClick() = node.globalLongClick()

    override fun tryClick() = node.tryClick()

    override fun click() = node.tryClick()

    @RequiresApi(Build.VERSION_CODES.N)
    override fun globalClick() = node.globalClick()

    override fun longClick() = node.longClick()

    override fun doubleClick() = node.doubleClick()

    override fun tryLongClick() = node.tryLongClick()

    override fun getCenterPoint() = node.getCenterPoint()

    override fun select() = node.select()

    override var hintText: CharSequence?
        @RequiresApi(Build.VERSION_CODES.O)
        get() = node.hintText
        @RequiresApi(Build.VERSION_CODES.O)
        set(value) {
            node.hintText = value
        }

    override var progress: Float
        @RequiresApi(Build.VERSION_CODES.KITKAT)
        get() = node.progress
        @RequiresApi(Build.VERSION_CODES.N)
        set(value) {
            node.progress = value
        }

    override val rangeInfo: AccessibilityNodeInfoCompat.RangeInfoCompat
        @RequiresApi(Build.VERSION_CODES.KITKAT)
        get() = node.rangeInfo

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    override fun setSelection(start: Int, end: Int): Boolean = node.setSelection(start, end)

    override fun clearSelection(): Boolean = node.clearSelection()

    override fun trySelect() = node.trySelect()

    @RequiresApi(Build.VERSION_CODES.M)
    override fun scrollUp() = node.scrollUp()

    @RequiresApi(Build.VERSION_CODES.M)
    override fun scrollDown() = node.scrollDown()

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun appendText(s: CharSequence) = node.appendText(s)

    override fun desc() = node.desc()

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun trySetText(text: CharSequence) = node.trySetText(text)

    override var text: CharSequence?
        get() = node.text
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        set(v) {
            node.text = v
        }

    override fun focus() = node.focus()

    override fun clearFocus() = node.clearFocus()

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun scrollForward() = node.scrollForward()

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun scrollBackward() = node.scrollBackward()

    @RequiresApi(Build.VERSION_CODES.M)
    override fun scrollLeft() = node.scrollLeft()

    @RequiresApi(Build.VERSION_CODES.N)
    override fun swipeOffset(dx: Int, dy: Int, delay: Int) = node.swipeOffset(dx, dy, delay)

    @RequiresApi(Build.VERSION_CODES.M)
    override fun scrollRight() = node.scrollRight()

    override val children get() = node.children

    override fun childAt(i: Int) = node.childAt(i)

    override val bounds get() = node.bounds

    override val boundsInParent get() = node.boundsInParent

    override val parent get() = node.parent

    override val childCount: Int get() = node.childCount

    override fun isClickable() = node.isClickable()

    override fun finder() = node.finder()

    override var isVisibleToUser: Boolean
        get() = node.isVisibleToUser
        set(value) {
            node.isVisibleToUser = value
        }

    override fun refresh() = node.refresh()

    override val actionList get() = node.actionList

    @RequiresApi(Build.VERSION_CODES.R)
    override fun sendImeAction() = node.sendImeAction()

    override val previousSibling: ViewNode?
        get() = node.previousSibling
    override val nextSibling: ViewNode?
        get() = node.nextSibling
}

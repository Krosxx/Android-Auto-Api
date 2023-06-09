package cn.vove7.auto.core.viewnode

import android.graphics.Point
import android.graphics.Rect
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import cn.vove7.auto.core.utils.ViewChildList
import cn.vove7.auto.core.viewfinder.SmartFinder

/**
 * ViewNode 操作方法集合
 */
interface ViewOperation {
    val id: String?
    val className: String?
    val packageName: String?

    val simpleName: String?
        get() = className?.let { it.substring(it.lastIndexOf('.') + 1) }


    /**
     * 尝试点击
     * 当此节点点击失败，会尝试向上级容器尝试
     * @return Boolean 是否成功
     */
    fun tryClick(): Boolean

    /**
     * 获取中心点坐标(绝对)
     * @return Point?
     */
    fun getCenterPoint(): Point

    /**
     * 获取下级所有Node
     * @return Array<ViewNode>
     */
    val children: ViewChildList

    /**
     * 获取边界范围
     * @return Rect
     */
    val bounds: Rect

    /**
     * 获取基于父级容器边界范围
     * @return Rect
     */
    val boundsInParent: Rect

    /**
     * 获取父级Node
     * @return ViewNode?
     */
    val parent: ViewNode?

    // 兄弟结点
    val previousSibling: ViewNode?
    val nextSibling: ViewNode?

    val requireParent: ViewNode
        get() {
            return parent ?: throw NullPointerException("parent is null of $this")
        }

    /**
     * 点击此Node
     * 失败率较高，使用起来不方便
     * @return Boolean
     */
    fun click(): Boolean

    /**
     * 使用全局函数click进行点击操作，如点击网页控件
     * 需要7.0+
     * @return Boolean
     */
    fun globalClick(): Boolean

    suspend fun globalLongClick(): Boolean

    /**
     * 以此Node中心滑动到dx,dy的地方
     * setScreenSize() 对此有效
     * @param dx Int x方向 移动距离 ±
     * @param dy Int y ±
     * @param delay Int 用时
     * @return Boolean
     */
    fun swipeOffset(dx: Int, dy: Int, delay: Int): Boolean

    /**
     * 尝试长按，机制类似tryClick
     * @return Boolean
     */
    fun tryLongClick(): Boolean

    /**
     * 获取下级Node数量
     * @return Int?
     */
    val childCount: Int

    fun childAt(i: Int): ViewNode?

    fun requireChildAt(i: Int): ViewNode {
        return try {
            childAt(i) ?: throw NullPointerException("child is null at $i of $this")
        } catch (e: IndexOutOfBoundsException) {
            throw IndexOutOfBoundsException("get child is out of bounds, index: $i, size: $childCount of $this")
        }
    }

    /**
     * 长按操作
     * @return Boolean
     */
    fun longClick(): Boolean

    /**
     * 双击操作
     * 默认使用tryClick
     * @return Boolean
     */
    fun doubleClick(): Boolean

    /**
     * 尝试设置文本内容，机制同tryClick
     * @param text String
     * @return Boolean 是否成功
     */
    fun trySetText(text: CharSequence): Boolean

    /**
     * 获取Node包含文本
     * @return String?
     */
    var text: CharSequence?

    var hintText: CharSequence?

    var progress: Float

    val rangeInfo: AccessibilityNodeInfoCompat.RangeInfoCompat

    /**
     * 追加文本
     * @param s String
     * @return Boolean
     */
    fun appendText(s: CharSequence)

    fun desc(): CharSequence?

    // 选择
    fun select(): Boolean

    fun setSelection(start: Int, end: Int): Boolean
    fun clearSelection(): Boolean

    fun trySelect(): Boolean

    // 获得焦点
    fun focus(): Boolean

    fun clearFocus(): Boolean

    /***以下不常用***/

    // 一般
    fun scrollUp(): Boolean

    fun scrollDown(): Boolean

    fun scrollForward(): Boolean
    fun scrollBackward(): Boolean
    fun scrollLeft(): Boolean
    fun scrollRight(): Boolean

    fun isClickable(): Boolean

    var isVisibleToUser: Boolean

    fun finder(): SmartFinder

    fun refresh(): Boolean

    val actionList: List<AccessibilityActionCompat>

    fun sendImeAction(): Boolean

}
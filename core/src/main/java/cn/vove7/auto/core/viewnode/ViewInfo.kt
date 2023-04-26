package cn.vove7.auto.core.viewnode

import android.graphics.Bitmap
import android.graphics.Rect

/**
 * # ViewInfo
 * (权)属性： 1位置     3文字  1类型
 * 特征:  是否位于  相似  相同
 * @author Administrator
 * 2018/11/1
 */
class ViewInfo(
        val text: CharSequence?,
        val desc:CharSequence?,
        val type: CharSequence?,
        val boundsInParent: Rect,
        val boundsInScreen: Rect,
//        val clickable: Boolean,
        val image: Bitmap? = null
//        val canPopup: Boolean
) {
    override fun toString(): String {
        return "$text\t$type\t$boundsInScreen\t$desc"
    }
}
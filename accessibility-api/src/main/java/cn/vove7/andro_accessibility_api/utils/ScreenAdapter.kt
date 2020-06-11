package cn.vove7.andro_accessibility_api.utils

import android.content.Context
import android.graphics.Point
import android.graphics.RectF
import android.util.DisplayMetrics
import android.util.Pair
import android.view.WindowManager
import cn.vove7.andro_accessibility_api.InitCp

/**
 * # ScreenAdapter
 *
 * @author 17719247306
 * 2018/9/6
 */
object ScreenAdapter {
    private var deviceHeight = 1920
    private var deviceWidth = 1080

    init {
        val m = DisplayMetrics()
        (InitCp.AppIns.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            .defaultDisplay.getRealMetrics(m)
        deviceHeight = m.heightPixels
        deviceWidth = m.widthPixels
    }

    var relHeight =
        deviceHeight
    var relWidth =
        deviceWidth

    fun setScreenSize(width: Int, height: Int) {
        relHeight = height
        relWidth = width
    }

    fun getRelPoint(p: Point): Point {
        return Point(
            p.x * relWidth / deviceWidth,
            p.y * relHeight / deviceHeight
        )
    }

    fun reSet() {
        relHeight =
            deviceHeight
        relWidth =
            deviceWidth
    }


    fun scalePoints(points: Array<Pair<Int, Int>>): Array<Pair<Float, Float>> {

        val ps = Array(points.size) { Pair(0f, 0f) }

        val index = 0
        points.forEach {
            val x =
                scaleX(it.first)
            val y =
                scaleY(it.second)
            ps[index] = Pair(x, y)
        }
        return ps
    }

    fun scaleX(x: Int): Float = scaleX(x.toFloat())

    fun scaleY(y: Int): Float = scaleY(y.toFloat())

    fun scaleX(x: Float): Float {
        return (x / relWidth * deviceWidth)
    }

    fun scaleY(y: Float): Float {
        return (y / relHeight * deviceHeight)
    }
}

class AdapterRectF : RectF {

    constructor(left: Float, top: Float, right: Float, bottom: Float) : super(
        ScreenAdapter.scaleX(left),
        ScreenAdapter.scaleY(top),
        ScreenAdapter.scaleX(right),
        ScreenAdapter.scaleY(bottom)
    )

    constructor(r: AdapterRectF) : super(r)

    constructor(r: RectF) : super(
        ScreenAdapter.scaleX(r.left),
        ScreenAdapter.scaleY(r.top),
        ScreenAdapter.scaleX(r.right),
        ScreenAdapter.scaleY(r.bottom)
    )

    override fun set(left: Float, top: Float, right: Float, bottom: Float) {
        super.set(
            ScreenAdapter.scaleX(left),
            ScreenAdapter.scaleY(top),
            ScreenAdapter.scaleX(right),
            ScreenAdapter.scaleY(bottom)
        )
    }
}


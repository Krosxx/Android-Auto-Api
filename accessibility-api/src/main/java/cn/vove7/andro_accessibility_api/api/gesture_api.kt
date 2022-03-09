package cn.vove7.andro_accessibility_api.api

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Pair
import android.view.ViewConfiguration
import androidx.annotation.RequiresApi
import cn.vove7.andro_accessibility_api.AccessibilityApi
import cn.vove7.andro_accessibility_api.utils.NeedAccessibilityException
import cn.vove7.andro_accessibility_api.utils.ResultBox
import cn.vove7.andro_accessibility_api.utils.ScreenAdapter

/**
 * 手势api
 */

private val gestureService: AccessibilityService
    get() = if (AccessibilityApi.isGestureServiceEnable
        && Build.VERSION.SDK_INT > Build.VERSION_CODES.N
    ) AccessibilityApi.requireGesture
    else throw NeedAccessibilityException("高级无障碍服务未开启 或 系统低于 Android N")

/**
 * 设置屏幕相对坐标
 * @param width Int
 * @param height Int
 */
fun setScreenSize(width: Int, height: Int) = ScreenAdapter.setScreenSize(width, height)

fun resetScreenSize() = ScreenAdapter.reset()

/**
 * 根据点坐标生成路径 执行手势
 * @param duration Long
 * @param points Array<Pair<Int, Int>>
 * @param onCancel  操作被打断
 * @return Boolean
 */
@RequiresApi(Build.VERSION_CODES.N)
fun gesture(
    duration: Long,
    points: Array<Pair<Int, Int>>,
    onCancel: Function0<Unit>? = null
): Boolean {
    val path = pointsToPath(points)
    return playGestures(listOf(GestureDescription.StrokeDescription(path, 0, duration)), onCancel)
}

/**
 * 根据Path执行手势
 * @param duration Long
 * @param path Path
 * @param onCancel Function0<Unit>?
 * @return Boolean
 */
@RequiresApi(Build.VERSION_CODES.N)
fun gesture(
    duration: Long, path: Path,
    onCancel: Function0<Unit>? = null
): Boolean {
    return playGestures(listOf(GestureDescription.StrokeDescription(path, 0, duration)), onCancel)
}

/**
 * 多路径手势
 * @param duration Long
 * @param paths Array<Path>
 * @param onCancel Function0<Unit>?
 * @return Boolean
 */
@RequiresApi(Build.VERSION_CODES.N)
fun gesture(
    duration: Long, paths: Array<Path>,
    onCancel: Function0<Unit>? = null
): Boolean {
    requireGestureAccessibility()
    return playGestures(
        paths.map { GestureDescription.StrokeDescription(it, 0, duration) },
        onCancel
    )
}

/**
 * api 异步手势
 * @param duration Long
 * @param points Array<Pair<Int, Int>>
 */
@RequiresApi(api = Build.VERSION_CODES.N)
fun gestureAsync(
    duration: Long,
    points: Array<Pair<Int, Int>>
) {
    val path = pointsToPath(points)
    doGesturesAsync(listOf(GestureDescription.StrokeDescription(path, 0, duration)))
}

/**
 * api 多路径手势
 * @param duration Long
 * @param ppss Array<Array<Pair<Int, Int>>>
 */
@RequiresApi(Build.VERSION_CODES.N)
fun gestures(
    duration: Long, ppss: Array<Array<Pair<Int, Int>>>,
    onCancel: Function0<Unit>? = null
): Boolean {
    val list = mutableListOf<GestureDescription.StrokeDescription>()
    ppss.forEach {
        list.add(GestureDescription.StrokeDescription(pointsToPath(it), 0, duration))
    }
    return playGestures(list, onCancel)
}

/**
 * api 多路径手势 异步
 * @param duration Long
 * @param ppss Array<Array<Pair<Int, Int>>>
 */
@RequiresApi(Build.VERSION_CODES.N)
fun gesturesAsync(duration: Long, ppss: Array<Array<Pair<Int, Int>>>) {
    val list = mutableListOf<GestureDescription.StrokeDescription>()
    ppss.forEach {
        list.add(GestureDescription.StrokeDescription(pointsToPath(it), 0, duration))
    }
    doGesturesAsync(list)
}

/**
 * 同步
 * @param strokes Array<out StrokeDescription>
 * @return Boolean
 */
@RequiresApi(api = Build.VERSION_CODES.N)
fun playGestures(
    strokeList: List<GestureDescription.StrokeDescription>,
    onCancel: Function0<Unit>? = null
): Boolean {
    val builder = GestureDescription.Builder()
    for (stroke in strokeList) {
        builder.addStroke(stroke)
    }
    return doGestures(builder.build(), onCancel)
}

/**
 * 点转路径
 * @param points Array<Pair<Int, Int>>
 * @return Path
 */
private fun pointsToPath(points: Array<Pair<Int, Int>>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(ScreenAdapter.scaleX(points[0].first), ScreenAdapter.scaleY(points[0].second))

    for (i in 1 until points.size) {
        path.lineTo(ScreenAdapter.scaleX(points[i].first), ScreenAdapter.scaleY(points[i].second))
    }
    return path
}

/**
 * 同步手势
 * @param description GestureDescription
 * @return Boolean
 */
@RequiresApi(api = Build.VERSION_CODES.N)
private fun doGestures(
    description: GestureDescription,
    onCancel: Function0<Unit>?
): Boolean {
    // 主线程不指定Handler
    val handler = if (Looper.myLooper() == Looper.getMainLooper()) null
    else HandlerThread("ges").let {
        it.start()
        Handler(it.looper)
    }
    val result = ResultBox(false)
    gestureService.dispatchGesture(
        description,
        object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                result.setAndNotify(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                onCancel?.invoke()
                result.setAndNotify(false)
            }
        }, handler
    ).also {
        if (!it) {
            return false
        }
    }
    return (result.blockedGet() ?: false).also {
        //结束 HanderThread
        handler?.looper?.quitSafely()
    }
}

/**
 * 异步手势
 * @param strokeList List<out StrokeDescription>
 */
@RequiresApi(api = Build.VERSION_CODES.N)
fun doGesturesAsync(strokeList: List<GestureDescription.StrokeDescription>) {
    val builder = GestureDescription.Builder()
    for (stroke in strokeList) {
        builder.addStroke(stroke)
    }
    gestureService.dispatchGesture(builder.build(), null, null)
}

@RequiresApi(Build.VERSION_CODES.N)
fun click(x: Int, y: Int): Boolean =
    pressWithTime(x, y, ViewConfiguration.getTapTimeout() + 50)

/**
 *
 * @param x Int
 * @param y Int
 * @param delay Int
 * @return Boolean
 */
@RequiresApi(Build.VERSION_CODES.N)
fun pressWithTime(x: Int, y: Int, delay: Int): Boolean {
    return gesture(delay.toLong(), arrayOf(Pair(x, y)))
}

/**
 * 长按 相对坐标
 * @param x Int
 * @param y Int
 * @return Boolean
 */
@RequiresApi(Build.VERSION_CODES.N)
fun longClick(x: Int, y: Int) = pressWithTime(
    x, y, (ViewConfiguration.getLongPressTimeout() + 200)
)

/**
 * 两点间滑动
 * @param x1 Int
 * @param y1 Int
 * @param x2 Int
 * @param y2 Int
 * @param dur Int
 * @return Boolean
 */
@RequiresApi(Build.VERSION_CODES.N)
fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, dur: Int): Boolean =
    gesture(
        dur.toLong(), arrayOf(
            Pair(x1, y1),
            Pair(x2, y2)
        )
    )

@RequiresApi(Build.VERSION_CODES.N)
fun scrollUp(): Boolean {
    val mtop = (ScreenAdapter.relHeight * 0.1).toInt()
    val mBottom = (ScreenAdapter.relHeight * 0.85).toInt()
    val xCenter = (ScreenAdapter.relWidth * 0.5).toInt()

    return swipe(xCenter, mBottom, xCenter, mtop, 400)
}

@RequiresApi(Build.VERSION_CODES.N)
fun scrollDown(): Boolean {
    val mtop = (ScreenAdapter.relHeight * 0.15).toInt()
    val mBottom = (ScreenAdapter.relHeight * 0.9).toInt()
    val xCenter = (ScreenAdapter.relWidth * 0.5).toInt()
    return swipe(xCenter, mtop, xCenter, mBottom, 400)
}

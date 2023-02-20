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
import cn.vove7.andro_accessibility_api.utils.GestureCanceledException
import cn.vove7.andro_accessibility_api.utils.NeedAccessibilityException
import cn.vove7.andro_accessibility_api.utils.ScreenAdapter
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

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
 * @return Boolean
 */
@RequiresApi(Build.VERSION_CODES.N)
suspend fun gesture(
    duration: Long,
    points: Array<Pair<Int, Int>>
): Boolean {
    val path = pointsToPath(points)
    return playGestures(listOf(GestureDescription.StrokeDescription(path, 0, duration)))
}

/**
 * 根据Path执行手势
 * @param duration Long
 * @param path Path
 * @return Boolean
 */
@RequiresApi(Build.VERSION_CODES.N)
suspend fun gesture(
    duration: Long, path: Path
) = gesture(duration, arrayOf(path))

/**
 * 多路径手势
 * @param duration Long
 * @param paths Array<Path>
 * @return Boolean
 */
@RequiresApi(Build.VERSION_CODES.N)
suspend fun gesture(
    duration: Long, paths: Array<Path>
) = playGestures(
    paths.map { GestureDescription.StrokeDescription(it, 0, duration) }
)

/**
 * api 异步手势
 * @param duration Long
 * @param points Array<Pair<Int, Int>>
 */
@RequiresApi(api = Build.VERSION_CODES.N)
fun gestureAsync(
    duration: Long, path: Path,
    callback: AccessibilityService.GestureResultCallback? = null
) = gestureAsync(duration, arrayOf(path), callback)

@RequiresApi(api = Build.VERSION_CODES.N)
fun gestureAsync(
    duration: Long, paths: Array<Path>,
    callback: AccessibilityService.GestureResultCallback? = null
) = doGesturesAsync(
    paths.map { GestureDescription.StrokeDescription(it, 0, duration) },
    callback
)

@RequiresApi(api = Build.VERSION_CODES.N)
fun gestureAsync(
    duration: Long,
    points: Array<Pair<Int, Int>>,
    callback: AccessibilityService.GestureResultCallback? = null
) {
    val path = pointsToPath(points)
    doGesturesAsync(
        listOf(GestureDescription.StrokeDescription(path, 0, duration)),
        callback
    )
}

/**
 * api 多路径手势
 * @param duration Long
 * @param ppss Array<Array<Pair<Int, Int>>>
 */
@RequiresApi(Build.VERSION_CODES.N)
suspend fun gestures(
    duration: Long, ppss: Array<Array<Pair<Int, Int>>>,
) = runCatching {
    val list = mutableListOf<GestureDescription.StrokeDescription>()
    ppss.forEach {
        list.add(GestureDescription.StrokeDescription(pointsToPath(it), 0, duration))
    }
    return playGestures(list)
}.holdGestureResult()

/**
 * api 多路径手势 异步
 * @param duration Long
 * @param ppss Array<Array<Pair<Int, Int>>>
 */
@RequiresApi(Build.VERSION_CODES.N)
fun gesturesAsync(
    duration: Long, ppss: Array<Array<Pair<Int, Int>>>,
    callback: AccessibilityService.GestureResultCallback? = null
) {
    val list = mutableListOf<GestureDescription.StrokeDescription>()
    ppss.forEach {
        list.add(GestureDescription.StrokeDescription(pointsToPath(it), 0, duration))
    }
    doGesturesAsync(list, callback)
}

/**
 * 同步执行手势
 * @param strokeList Array<out StrokeDescription>
 * @return Boolean
 */
@RequiresApi(api = Build.VERSION_CODES.N)
suspend fun playGestures(
    strokeList: List<GestureDescription.StrokeDescription>
) = runCatching {
    val builder = GestureDescription.Builder()
    strokeList.forEach(builder::addStroke)
    return doGestures(builder.build())
}.holdGestureResult()

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
 * @return Boolean 执行是否成功，中断 return false
 */
@RequiresApi(api = Build.VERSION_CODES.N)
@Throws(GestureCanceledException::class)
private suspend fun doGestures(description: GestureDescription): Boolean {
    // 主线程不指定Handler
    requireGestureAccessibility()
    val handler = if (Looper.myLooper() == Looper.getMainLooper()) null
    else HandlerThread("ges").let {
        it.start()
        Handler(it.looper)
    }
    return suspendCoroutine { coroutine ->
        gestureService.dispatchGesture(
            description,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    coroutine.resume(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription) {
                    coroutine.resumeWithException(GestureCanceledException(gestureDescription))
                }
            }, handler
        )
    }.also {
        handler?.looper?.quitSafely()
    }
}

/**
 * 异步手势
 * @param strokeList List<out StrokeDescription>
 */
@RequiresApi(api = Build.VERSION_CODES.N)
fun doGesturesAsync(
    strokeList: List<GestureDescription.StrokeDescription>,
    callback: AccessibilityService.GestureResultCallback?
) {
    val builder = GestureDescription.Builder()
    strokeList.forEach(builder::addStroke)
    gestureService.dispatchGesture(builder.build(), callback, null)
}

@RequiresApi(Build.VERSION_CODES.N)
suspend fun click(x: Int, y: Int): Boolean =
    pressWithTime(x, y, ViewConfiguration.getTapTimeout() + 50)


private fun Result<Boolean>.holdGestureResult() =
    onFailure(Timber::w).fold(onFailure = { false }, onSuccess = { it })


/**
 *
 * @param x Int
 * @param y Int
 * @param delay Int
 * @return Boolean
 */
@RequiresApi(Build.VERSION_CODES.N)
suspend fun pressWithTime(x: Int, y: Int, delay: Int) =
    gesture(delay.toLong(), arrayOf(Pair(x, y)))

/**
 * 长按 相对坐标
 * @param x Int
 * @param y Int
 * @return Boolean
 */
@RequiresApi(Build.VERSION_CODES.N)
suspend fun longClick(x: Int, y: Int) = pressWithTime(
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
suspend fun swipe(
    x1: Int, y1: Int,
    x2: Int, y2: Int,
    dur: Int
): Boolean = gesture(
    dur.toLong(), arrayOf(
    Pair(x1, y1),
    Pair(x2, y2)
))

@RequiresApi(Build.VERSION_CODES.N)
suspend fun scrollUp(): Boolean = runCatching {
    val mtop = (ScreenAdapter.relHeight * 0.1).toInt()
    val mBottom = (ScreenAdapter.relHeight * 0.85).toInt()
    val xCenter = (ScreenAdapter.relWidth * 0.5).toInt()
    return swipe(xCenter, mBottom, xCenter, mtop, 400)
}.holdGestureResult()

@RequiresApi(Build.VERSION_CODES.N)
suspend fun scrollDown(): Boolean {
    val mtop = (ScreenAdapter.relHeight * 0.15).toInt()
    val mBottom = (ScreenAdapter.relHeight * 0.9).toInt()
    val xCenter = (ScreenAdapter.relWidth * 0.5).toInt()
    return swipe(xCenter, mtop, xCenter, mBottom, 400)
}

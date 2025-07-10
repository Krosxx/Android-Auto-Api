package cn.vove7.andro_accessibility_api.demo.actions

import android.content.Intent
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.util.Pair
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import cn.vove7.andro_accessibility_api.demo.DemoApp
import cn.vove7.andro_accessibility_api.demo.DrawableActivity
import cn.vove7.andro_accessibility_api.demo.MainActivity
import cn.vove7.andro_accessibility_api.demo.R
import cn.vove7.andro_accessibility_api.demo.toast
import cn.vove7.auto.core.AutoApi
import cn.vove7.auto.core.api.back
import cn.vove7.auto.core.api.buildLayoutInfo
import cn.vove7.auto.core.api.click
import cn.vove7.auto.core.api.editor
import cn.vove7.auto.core.api.findAllWith
import cn.vove7.auto.core.api.gesture
import cn.vove7.auto.core.api.gestureAsync
import cn.vove7.auto.core.api.gestures
import cn.vove7.auto.core.api.home
import cn.vove7.auto.core.api.matchesText
import cn.vove7.auto.core.api.powerDialog
import cn.vove7.auto.core.api.pullNotificationBar
import cn.vove7.auto.core.api.quickSettings
import cn.vove7.auto.core.api.recents
import cn.vove7.auto.core.api.setScreenSize
import cn.vove7.auto.core.api.waitForApp
import cn.vove7.auto.core.api.withId
import cn.vove7.auto.core.api.withText
import cn.vove7.auto.core.api.withType
import cn.vove7.auto.core.requireAutoService
import cn.vove7.auto.core.utils.AdapterRectF
import cn.vove7.auto.core.utils.AutoGestureDescription
import cn.vove7.auto.core.utils.GestureResultCallback
import cn.vove7.auto.core.utils.toDesc
import cn.vove7.auto.core.viewfinder.SF
import cn.vove7.auto.core.viewfinder.ScreenTextFinder
import cn.vove7.auto.core.viewfinder._desc
import cn.vove7.auto.core.viewfinder._text
import cn.vove7.auto.core.viewfinder.clickable
import cn.vove7.auto.core.viewfinder.containsText
import cn.vove7.auto.core.viewfinder.desc
import cn.vove7.auto.core.viewfinder.editable
import cn.vove7.auto.core.viewfinder.id
import cn.vove7.auto.core.viewfinder.longClickable
import cn.vove7.auto.core.viewfinder.matchText
import cn.vove7.auto.core.viewfinder.scrollable
import cn.vove7.auto.core.viewfinder.similarityText
import cn.vove7.auto.core.viewfinder.text
import cn.vove7.auto.core.viewfinder.textOrDesc
import cn.vove7.auto.core.viewfinder.type
import cn.vove7.auto.core.viewnode.ViewNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.coroutineContext

/**
 * # actoins
 *
 * Created on 2020/6/10
 * @author Vove
 */

class BaseNavigatorAction : Action() {
    override val name: String get() = "基础导航"

    override suspend fun run(act: ComponentActivity) {
        requireAutoService()
        toast("下拉通知栏..")
        pullNotificationBar()
        delay(1000)
        toast("快捷设置..")
        delay(1000)
        quickSettings()
        delay(1000)
        back()
        delay(500)
        back()
        delay(1000)
        powerDialog()
        delay(500)
        back()
        delay(1000)
        recents()
        delay(1000)
        back()
        delay(1000)
        home()
        delay(100)
        DemoApp.INS.startActivity(Intent(DemoApp.INS, MainActivity::class.java).also {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}

class PickScreenText : Action() {
    override val name: String
        get() = "提取屏幕文字"

    override suspend fun run(act: ComponentActivity) {
        val ts = ScreenTextFinder().find().joinToString("\n\n")
        withContext(Dispatchers.Main) {
            AlertDialog.Builder(act).apply {
                setTitle("提取文字：")
                setMessage(ts)
                show()
            }
        }
    }
}

class SiblingTestAction : Action() {
    override val name: String = "SiblingTest"

    override suspend fun run(act: ComponentActivity) {
        Timber.i(buildLayoutInfo())
        val view = withText(name).requireFirst()
        Timber.i("view: $view")
        Timber.i("view.previousSibling: ${view.previousSibling}")
        Timber.i("view.nextSibling: ${view.nextSibling}")
        check(view.previousSibling?.nextSibling == view)
        toast("assert success")
    }
}

class DrawableAction : Action() {
    override val name: String
        get() = "手势画图 - Rect - Circle - Oval"

    @RequiresApi(Build.VERSION_CODES.N)
    override suspend fun run(act: ComponentActivity) {
        act.startActivity(Intent(act, DrawableActivity::class.java))
        toast("1s后开始绘制，请不要触摸屏幕")
        delay(1000)

        // 设置相对屏幕 非必须
        setScreenSize(500, 500)
        // 指定点转路径手势
        if (!gesture(
                2000L, arrayOf(
                    100 t 100,
                    100 t 200,
                    200 t 200,
                    200 t 100,
                    100 t 100
                )
            )
        ) {
            toast("打断")
        }

        scaleGesture()
        delay(800)
        // 点击clear按钮
        withText("clear").tryClick()
        drawCircle()
        delay(800)
        withText("clear").tryClick()
        drawOval()

        delay(800)
        withText("clear").tryClick()
        drawCircleAsync()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    suspend fun drawCircle() {
        val p = Path().apply {
            addOval(RectF(500f, 500f, 800f, 800f), Path.Direction.CW)
        }
        if (!gesture(2000L, p)) {
            toast("打断")
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private suspend fun scaleGesture() {
        if (!gestures(
                800, arrayOf(
                    arrayOf(Pair(200, 200), Pair(100, 100)),
                    arrayOf(Pair(220, 220), Pair(300, 300)),
                )
            )
        ) {
            toast("scaleGesture 失败")
        }
        if (!gestures(
                800, arrayOf(
                    arrayOf(Pair(200, 200), Pair(100, 100)).reversedArray(),
                    arrayOf(Pair(220, 220), Pair(300, 300)).reversedArray(),
                )
            )
        ) {
            toast("scaleGesture 失败")
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    suspend fun drawCircleAsync() {
        val p = Path().apply {
            addOval(RectF(500f, 500f, 800f, 800f), Path.Direction.CW)
        }
        Timber.d("start gestureAsync")
        gestureAsync(2000L, p, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: AutoGestureDescription?) {
                toast("gestureAsync 完成")
            }

            override fun onCancelled(gestureDescription: AutoGestureDescription?) {
                toast("gestureAsync 中断")
            }
        })
        delay(800)
        // 测试手势中断
        Timber.d("gestureAsync cancel")
        click(250, 250)
        delay(3000)
    }

    // AdapterRectF 会根据设置的相对屏幕大小换算
    @RequiresApi(Build.VERSION_CODES.N)
    suspend fun drawOval() {
        val p = Path().apply {
            addOval(AdapterRectF(200f, 200f, 300f, 300f), Path.Direction.CW)
        }
        if (!gesture(2000L, p)) {
            toast("打断")
        }
    }

    infix fun <A, B> A.t(that: B): Pair<A, B> = Pair(this, that)

}

class WaitAppAction : Action() {
    override val name: String
        get() = "等待 Chrome 打开 - 展开菜单"

    override suspend fun run(act: ComponentActivity) {

        toast("start chrome after 1s")
        delay(1000)

        val targetApp = "com.android.chrome"
        act.startActivity(act.packageManager.getLaunchIntentForPackage(targetApp))

        if (
            waitForApp(targetApp, 5000).also {
                toast("wait " + if (it) "success" else "failed")
            }
        ) {
            withId("menu_button").tryClick()
        }
    }
}

class ViewFinderWithLambda : Action() {
    override val name: String
        get() = "ViewFinderWithLambda"

    override suspend fun run(act: ComponentActivity) {
        val s = findAllWith {
            Log.d(TAG, "${it.text} ${it.isClickable}")
            it.isClickable
        }.joinToString("\n\n")

        withContext(Dispatchers.Main) {
            AlertDialog.Builder(act).apply {
                setTitle("可点击节点：")
                setMessage(s)
                show()
            }
        }
    }
}

class TextMatchAction : Action() {
    override val name: String
        get() = "文本匹配"


    override suspend fun run(act: ComponentActivity) {
        val s = buildString {
            appendLine("containsText(\"基础\").find()")
            appendLine(SF.containsText("基础").find().map { it.text })
            appendLine()
            appendLine("matchesText(\"[a-zA-Z]+\").find()")
            appendLine(matchesText("[a-zA-Z]+").find().map { it.text })

        }
        withContext(Dispatchers.Main) {
            AlertDialog.Builder(act).apply {
                setTitle("可点击节点：")
                setMessage(s)
                show()
            }
        }
    }
}


val TAG = "Action"

class SelectTextAction : Action() {
    override val name: String
        get() = "编辑文本 - 清空文本 - 选择文本 0-5"

    override suspend fun run(act: ComponentActivity) {
        editor().require().apply {
            repeat(5) {
                appendText(".x")
                delay(500)
            }
            delay(1000)
            text = ""
            delay(1000)
            text = "123456"
            delay(1000)
            setSelection(0, 5)
            delay(1000)

            clearSelection()
            clearFocus()
        }
    }
}

class ClickTextAction : Action() {

    override val name: String
        get() = "点击文本"

    override suspend fun run(act: ComponentActivity) {
        val edit_text = act.findViewById<EditText>(R.id.edit_text)
        var targetText = edit_text.text.toString().trim()
        if (targetText == "123456") {
            targetText = "文本匹配"
            withContext(Dispatchers.Main) {
                edit_text.setText("文本匹配")
            }
        }
        if (targetText.isEmpty()) {
            toast("请输入文本")
            withContext(Dispatchers.Main) {
                edit_text.requestFocus()
            }
            return
        }
        val node = SF.containsText(targetText).type("textview")
        val t = node.findFirst()
        toast("haveFound: $t")
        delay(1000)
        toast("点击：${node.tryClick()}")
    }
}

class TraverseAllAction : Action() {
    override val name = "递归搜索视图包含"
    override suspend fun run(act: ComponentActivity) {

        Log.i(
            "TraverseAllAction",
            findAllWith {
                it.contentDescription != null
            }.joinToString("\n")
        )
        // assert = [ Bottom, SubView ]

    }
}

class SmartFinderAction : Action() {
    override val name = "SmartFinder测试"

    override suspend fun run(act: ComponentActivity) {
        val sb = StringBuilder()
        val node = SF.text("SmartFinder测试")
            .enableRootCompat()
            .findFirst()
        sb.appendLine(node?.toString())

        val orFinder = SF.text("123").or().id("text1").findFirst()
        sb.appendLine(orFinder?.toString())


        try {
            SF.find()
        } catch (e: Throwable) {
            sb.appendLine(e.message)
        }

        val customFinder = SF.containsText("Smart").where {
            (node?.text?.length ?: 5) > 3
        }.findFirst()
        sb.appendLine(customFinder?.toString())

        val f3 = SF(_text eq "aaa", _desc.."q", _desc contains "aa").findFirst()
        sb.appendLine(f3?.toString())

        val groupFinder = SF.containsText("Smart").or().id("111").findFirst()
        sb.appendLine(groupFinder?.toString())

        val s = ViewNode.findByDepths(1, 0, 0)
        sb.appendLine(s?.toString())

        (SF where text("1111") or text("2222")
                and id("111") or longClickable()).findAll()


        SF.where {
            it.isChecked
        }.find()

        // SF.where(IdCondition("view_id")).or(RTextEqCondition("[0-9]+")).find()
        // SF.id("view_id").or().matchText("[0-9]+").find()

        // group  (text=="111" && desc=="111") || (text=="222" && desc=="222")
        SF.where(SF.text("111").desc("111"))
            .or(SF.text("222").desc("222"))
            .find()

        AlertDialog.Builder(act).apply {
            setTitle("Output")
            setMessage(sb.toString())
            withContext(Dispatchers.Main) {
                show()
            }
        }
    }
}

class CoroutineStopAction : Action() {
    override val name = "协程测试"
    override suspend fun run(act: ComponentActivity) {
        val job = GlobalScope.async {
            val t = SF.containsText("周三").waitFor(10000)
            AlertDialog.Builder(act).apply {
                setTitle("Output")
                setMessage(t.toString())
                withContext(Dispatchers.Main) {
                    show()
                }
            }
        }
        val j = coroutineContext[Job]
        j?.invokeOnCompletion {
            job.cancel()
        }
        job.invokeOnCompletion {
            j?.cancel()
        }
        delay(3000)
        job.cancel()
    }
}

private suspend fun showInputDialog(act: ComponentActivity) = withContext(Dispatchers.Main) {
    val et = EditText(act)
    et.imeOptions = EditorInfo.IME_ACTION_SEARCH
    et.id = android.R.id.text1

    val dialog = AlertDialog.Builder(act).setView(et).show()

    et.setOnEditorActionListener { _, _, _ ->
        toast("Search call ${et.text}")
        dialog.dismiss()
        true
    }
    dialog to et
}

class SendImeAction : Action() {
    override val name: String
        get() = "SendImeAction"

    override suspend fun run(act: ComponentActivity) {
        showInputDialog(act)
        delay(800)
        SF.type("EditText").require(2000).apply {
            text = "123"
            tryClick()
            delay(300)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                sendImeAction()
            } else if (AutoApi.serviceType == AutoApi.SERVICE_TYPE_INSTRUMENTATION) {
                AutoApi.sendKeyCode(KeyEvent.KEYCODE_ENTER)
            }
        }
    }
}

class ToStringTestAction : Action() {
    override val name: String
        get() = "ToStringTest"

    fun logInfo(s: String) {
        Log.i("SF", s)
    }

    override suspend fun run(act: ComponentActivity) {

        val f1 = SF.id("111").textOrDesc("tede")
        logInfo(f1.toString())

        val f2 = SF.matchText("[0-9]+") or scrollable()
        logInfo(f2.toString())

        val f3 = SF(SF.id("11").editable()).and(SF.desc("11").clickable())
        logInfo(f3.toString())

        val f4 = SF.and(SF.id("1"))
        logInfo(f4.toString())

        val f5 = SF.id("1")
        logInfo(f5.toString())

        val f6 = SF.similarityText("111", 0.75f)
        logInfo(f6.toString())

    }
}

class InstrumentationSendKeyAction(
    override val name: String = "Instrumentation - SendKey"
) : Action() {
    override suspend fun run(act: ComponentActivity) {
        toast("back home")
        AutoApi.sendKeyCode(KeyEvent.KEYCODE_HOME)
    }
}

class InstrumentationSendTextAction(
    override val name: String = "Instrumentation - SendText"
) : Action() {
    override suspend fun run(act: ComponentActivity) {
        showInputDialog(act)
        delay(600)
        SF.editable().tryClick()
        delay(1000)
        // 不支持中文，输入文字可使用 ViewNode 操作
        AutoApi.sendString("123abc")
    }
}

class InstrumentationShotScreenAction(
    override val name: String = "Instrumentation - takeScreenshot"
) : Action() {
    override suspend fun run(act: ComponentActivity) {
        val screen = AutoApi.takeScreenshot()
        Timber.i("screen: $screen")
        withContext(Dispatchers.Main) {
            AlertDialog.Builder(act)
                .setTitle("截屏结果")
                .setView(ImageView(act).also {
                    it.setImageBitmap(screen)
                }).show()
        }
    }
}

class InstrumentationInjectInputEventAction(
    override val name: String = "Instrumentation - InjectInputEvent"
) : Action() {
    override suspend fun run(act: ComponentActivity) {
        var t = SystemClock.uptimeMillis()
        repeat(100) {
            val d = buildMotionEvent(
                t,
                when (it) {
                    0 -> MotionEvent.ACTION_DOWN
                    99 -> MotionEvent.ACTION_UP
                    else -> MotionEvent.ACTION_MOVE
                }, 100f, 5f * it
            )
            AutoApi.injectInputEvent(d, false)
            delay(10)
        }
        delay(1000)
        back()
        delay(100)

        showSeekbarDialog(act)
        delay(1000)
        val s = withType("SeekBar").requireFirst()

        val startX = s.bounds.left + 50f
        val y = s.bounds.centerY().toFloat()

        t = SystemClock.uptimeMillis()
        var d = buildMotionEvent(t, MotionEvent.ACTION_DOWN, startX, y)
        AutoApi.injectInputEvent(d, false)
        val endX = s.bounds.right - 50
        var px = startX
        repeat(2) {
            repeat(((endX - startX) / 10 / 2).toInt()) {
                px += 10
                delay(20)
                d = buildMotionEvent(t, MotionEvent.ACTION_MOVE, px, y)
                AutoApi.injectInputEvent(d, false)
            }
            if (it == 0) {
                toast("delay 1s")
                delay(1000)
            }
        }
        delay(2000)
        d = buildMotionEvent(t, MotionEvent.ACTION_UP, px, y)
        AutoApi.injectInputEvent(d, false)
    }

    private fun buildMotionEvent(downTime: Long, action: Int, x: Float, y: Float): MotionEvent {
        return MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0).also {
            it.source = InputDevice.SOURCE_TOUCHSCREEN
        }
    }
}

private suspend fun showSeekbarDialog(act: ComponentActivity) = withContext(Dispatchers.Main) {
    val seekBar = SeekBar(act)
    if (!act.isFinishing) {
        AlertDialog.Builder(act).setView(seekBar).show()
    }
}

class ContinueGestureAction(override val name: String = "ContinueGesture") : Action() {
    override suspend fun run(act: ComponentActivity) {
        showSeekbarDialog(act)
        delay(1000)
        val s = withType("SeekBar").require(2000)

        val startX = s.bounds.left + 50f
        val y = s.bounds.centerY().toFloat()

        val endX = s.bounds.right - 50

        val p1 = Path().apply {
            moveTo(startX, y)
            lineTo((endX + startX) / 2, y)
        }
        val stroke = AutoGestureDescription.StrokeDescription(p1, 0, 600, true)
        Timber.i("stroke id: ${stroke.id}")
        var r = AutoApi.doGestureSync(stroke.toDesc())
        Timber.i("1 $r")
        delay(200)

        val p2 = Path().apply {
            moveTo((endX + startX) / 2, y)
            lineTo(endX.toFloat(), y)
        }
        // 无障碍模式下 Android 8.0 以下 无效
        // uiauto 模式无视 警告
        val continueStroke = stroke.continueStroke(p2, 0, 600, false)
        Timber.i("continuedStrokeId: ${continueStroke.continuedStrokeId}")
        r = AutoApi.doGestureSync(continueStroke.toDesc())
        Timber.i("2 $r")

        delay(500)
        val p3 = Path().apply {
            moveTo(endX.toFloat(), y)
            lineTo((endX + startX) / 2, y)
        }
        val stroke3 = AutoGestureDescription.StrokeDescription(p3, 0, 600, true)
        r = AutoApi.doGestureSync(stroke3.toDesc())
        Timber.i("3 $r")
        delay(500)

        val s4 = stroke3.continueStroke(
            Path().apply {
                moveTo((endX + startX) / 2, y)
                lineTo(startX, y)
            }, 0, 500, false
        )
        r = AutoApi.doGestureSync(s4.toDesc())
        Timber.i("4 $r")
        delay(500)
    }
}

class ScreenshotAction : Action() {

    override val name: String
        get() = "Screenshot"

    override suspend fun run(act: ComponentActivity) {
        val bm = AutoApi.takeScreenshot()

        withContext(Dispatchers.Main) {
            AlertDialog.Builder(act)
                .setTitle("Screenshot Test")
                .setView(ImageView(act).apply {
                    setImageBitmap(bm)
                })
                .show()
        }

    }
}
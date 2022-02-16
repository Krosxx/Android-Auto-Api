package cn.vove7.andro_accessibility_api.demo.actions

import android.app.Activity
import android.content.Intent
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.util.Log
import android.util.Pair
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import cn.vove7.andro_accessibility_api.api.*
import cn.vove7.andro_accessibility_api.demo.*
import cn.vove7.andro_accessibility_api.ext.ScreenTextFinder
import cn.vove7.andro_accessibility_api.utils.AdapterRectF
import cn.vove7.andro_accessibility_api.viewfinder.*
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext

/**
 * # actoins
 *
 * Created on 2020/6/10
 * @author Vove
 */

class BaseNavigatorAction : Action() {
    override val name: String get() = "基础导航"

    override suspend fun run(act: Activity) {
        requireBaseAccessibility(true)
        toast("下拉通知栏..")
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

    override suspend fun run(act: Activity) {
        requireBaseAccessibility(true)
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

class DrawableAction : Action() {
    override val name: String
        get() = "手势画图 - Rect - Circle - Oval"

    @RequiresApi(Build.VERSION_CODES.N)
    override suspend fun run(act: Activity) {
        requireBaseAccessibility(true)
        requireGestureAccessibility()
        act.startActivity(Intent(act, DrawableActivity::class.java))
        toast("1s后开始绘制，请不要触摸屏幕")
        delay(1000)

        //设置相对屏幕 非必须
        setScreenSize(500, 500)
        //指定点转路径手势
        gesture(
            2000L, arrayOf(
                100 t 100,
                100 t 200,
                200 t 200,
                200 t 100,
                100 t 100
            )
        )
        delay(800)
        //点击clear按钮
        withText("clear").tryClick()
        drawCircle()
        delay(800)
        withText("clear").tryClick()
        drawOval()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun drawCircle() {
        val p = Path().apply {
            addOval(RectF(500f, 500f, 800f, 800f), Path.Direction.CW)
        }
        gesture(2000L, p) {
            toast("打断")
        }
    }

    //AdapterRectF 会根据设置的相对屏幕大小换算
    @RequiresApi(Build.VERSION_CODES.N)
    fun drawOval() {
        val p = Path().apply {
            addOval(AdapterRectF(200f, 200f, 300f, 300f), Path.Direction.CW)
        }
        gesture(2000L, p)
    }

    infix fun <A, B> A.t(that: B): Pair<A, B> = Pair(this, that)

}

class WaitAppAction : Action() {
    override val name: String
        get() = "等待 Chrome 打开 - 展开菜单"

    override suspend fun run(act: Activity) {
        waitBaseAccessibility()

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

    override suspend fun run(act: Activity) {
        requireBaseAccessibility(true)
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


    override suspend fun run(act: Activity) {
        requireBaseAccessibility(true)
        val s = buildString {
            appendln("containsText(\"基础\").find()")
            appendln(containsText("基础").find().map { it.text })
            appendln()
            appendln("matchesText(\"[a-zA-Z]+\").find()")
            appendln(matchesText("[a-zA-Z]+").find().map { it.text })

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

    override suspend fun run(act: Activity) {
        requireBaseAccessibility(true)
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

    override suspend fun run(act: Activity) {
        requireBaseAccessibility(true)
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
        val node = containsText(targetText).type("textview")
        val t = node.findFirst()
        toast("haveFound: $t")
        delay(1000)
        t?.tryClick()
    }
}

class TraverseAllAction : Action() {
    override val name = "递归搜索视图包含"
    override suspend fun run(act: Activity) {

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

    override suspend fun run(act: Activity) {
        requireBaseAccessibility(true)
        val sb = StringBuilder()
        val node = SF.text("SmartFinder测试").findFirst()
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

        val f3 = SG(_text eq "aaa", _desc.."q", _desc contains "aa").findFirst()
        sb.appendLine(f3?.toString())

        val groupFinder = SG(SF.containsText("Smart").or().id("111")).findFirst()
        sb.appendLine(groupFinder?.toString())

        val s = SF.findByDepths(1, 0, 0)
        sb.appendLine(s?.toString())

        SF.where {
            it.isChecked
        }.find()

//        SF.where(IdCondition("view_id")).or(RTextEqCondition("[0-9]+")).find()
//        SF.id("view_id").or().matchText("[0-9]+").find()

        //group  (text=="111" && desc=="111") || (text=="222" && desc=="222")
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

class CorourtineStopAction : Action() {
    override val name = "协程测试"
    override suspend fun run(act: Activity) {
        requireBaseAccessibility(true)
        val job = GlobalScope.async {
            val t = SF.attachCoroutine()
                .containsText("周三").waitFor(10000)
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
package cn.vove7.andro_accessibility_api.demo

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import cn.vove7.andro_accessibility_api.demo.actions.*
import kotlinx.coroutines.Job

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val actions = mutableListOf(
            BaseNavigatorAction(),
            PickScreenText(),
            DrawableAction(),
            WaitAppAction(),
            SelectTextAction(),
            ViewFinderWithLambda(),
            TextMatchAction(),
            ClickTextAction(),
            TraverseAllAction(),
            SmartFinderAction(),
            CoroutineStopAction(),
            ToStringTestAction(),
            object : Action() {
                override val name = "Stop"
                override suspend fun run(act: Activity) {}
            }
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            actions.add(SendImeAction())
        }

        val lv = findViewById<ListView>(R.id.list_view)

        lv.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, actions)
        lv.setOnItemClickListener { _, _, position, _ ->
            onActionClick(actions[position])
        }
    }

    var actionJob: Job? = null

    private fun onActionClick(action: Action) {
        if (action.name == "Stop") {
            actionJob?.cancel()
            return
        }
        if (actionJob?.isCompleted.let { it != null && !it }) {
            toast("有正在运行的任务")
            return
        }
        actionJob = launchWithExpHandler {
            action.run(this@MainActivity)
        }
        actionJob?.invokeOnCompletion {
            toast("执行结束")
        }
    }

    override fun onDestroy() {
        actionJob?.cancel()
        super.onDestroy()
    }
}

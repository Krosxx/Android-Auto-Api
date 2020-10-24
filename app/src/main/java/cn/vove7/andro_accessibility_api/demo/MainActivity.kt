package cn.vove7.andro_accessibility_api.demo

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import cn.vove7.andro_accessibility_api.demo.actions.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Job

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        list_view.adapter = ActionAdapter(::onActionClick)
    }

    var actionJob: Job? = null

    private fun onActionClick(action: Action) {
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

class ActionAdapter(val onActionClick: (action: Action) -> Unit) :
    BaseAdapter() {
    private val actions: List<Action>

    init {
        actions = buildActions()
    }

    private fun buildActions(): List<Action> {
        return listOf(
            BaseNavigatorAction(),
            PickScreenText(),
            DrawableAction(),
            WaitAppAction(),
            SelectTextAction(),
            ViewFinderWithLambda(),
            TextMatchAction(),
            ClickTextAction(),
            TraverseAllAction(),
        )
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        return convertView ?: (TextView(parent?.context).apply {
            setPadding(50, 50, 50, 50)
        }).apply {
            text = actions[position].name
            setOnClickListener {
                onActionClick(actions[position])
            }
        }
    }

    override fun getItem(position: Int): Any = actions[position]
    override fun getItemId(position: Int): Long = position.toLong()
    override fun getCount(): Int = actions.size
}
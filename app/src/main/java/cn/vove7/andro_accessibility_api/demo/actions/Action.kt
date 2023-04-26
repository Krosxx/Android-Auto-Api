package cn.vove7.andro_accessibility_api.demo.actions

import androidx.activity.ComponentActivity

/**
 * # Action
 *
 * Created on 2020/6/10
 * @author Vove
 */
abstract class Action {
    abstract val name: String
    abstract suspend fun run(act: ComponentActivity)

    override fun toString() = name

}
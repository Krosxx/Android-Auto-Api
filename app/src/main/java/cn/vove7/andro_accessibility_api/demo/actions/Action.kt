package cn.vove7.andro_accessibility_api.demo.actions

import android.app.Activity

/**
 * # Action
 *
 * Created on 2020/6/10
 * @author Vove
 */
abstract class Action {
    abstract val name: String
    abstract suspend fun run(act: Activity)

    override fun toString() = name

}
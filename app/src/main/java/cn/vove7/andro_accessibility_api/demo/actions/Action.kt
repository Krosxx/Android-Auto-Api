package cn.vove7.andro_accessibility_api.demo.actions

import android.app.Activity

/**
 * # Action
 *
 * Created on 2020/6/10
 * @author Vove
 */
interface Action {
    val name: String
    suspend fun run(act: Activity)
}
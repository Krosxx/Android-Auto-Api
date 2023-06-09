package cn.vove7.andro_accessibility_api.demo

import cn.vove7.accessibility.uiauto.AutoInstrumentation

/**
 * # MyInstrumentation
 *
 * @author Vove
 * @date 2023/4/25
 */
@Suppress("unused")
class MyInstrumentation : AutoInstrumentation() {

    override fun onStart() {
        super.onStart()
        startActivitySync(context.packageManager.getLaunchIntentForPackage(context.packageName))
    }


}
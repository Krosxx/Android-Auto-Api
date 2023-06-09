package cn.vove7.accessibility.uiauto

import android.os.RemoteException

/**
 * # GestureCallback
 *
 * @author Vove
 * @date 2023/4/26
 */
fun interface GestureCallback {

    fun onPerformGestureResult(sequence: Int, completedSuccessfully: Boolean)
}
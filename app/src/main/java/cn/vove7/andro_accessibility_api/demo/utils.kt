package cn.vove7.andro_accessibility_api.demo

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * # utils
 *
 * Created on 2020/6/11
 * @author Vove
 */


fun launchWithExpHandler(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
) = GlobalScope.launch(context + ExceptionHandler, start, block)


val ExceptionHandler by lazy {
    CoroutineExceptionHandler { _, throwable ->
        toast("执行失败： ${throwable.message ?: "$throwable"}")
        throwable.printStackTrace()
    }
}

val mainHandler by lazy {
    Handler(Looper.getMainLooper())
}

fun runOnUi(block: () -> Unit) {
    if (Looper.getMainLooper() == Looper.myLooper()) {
        block()
    } else {
        mainHandler.post(block)
    }
}


fun toast(m: String) =
    runOnUi {
        Toast.makeText(DemoApp.INS, m, Toast.LENGTH_SHORT).show()
    }

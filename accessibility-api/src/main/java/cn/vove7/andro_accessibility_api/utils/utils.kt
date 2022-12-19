package cn.vove7.andro_accessibility_api.utils

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import cn.vove7.andro_accessibility_api.InitCp
import cn.vove7.andro_accessibility_api.viewfinder.ViewFinder
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import java.util.Locale
import kotlin.coroutines.coroutineContext
import kotlin.math.max

/**
 * # utils
 *
 * Created on 2020/6/10
 * @author Vove
 */


/**
 * 循环执行等待结果；超时返回空
 * eg用于视图搜索
 * @param waitMillis Long
 * @param run () -> T 返回空时，重新执行，直到超时
 * @return T
 */
suspend fun <T> whileWaitTime(
    waitMillis: Long,
    interval: Long = 0L, run: suspend () -> T?
): T? {
    val begin = SystemClock.elapsedRealtime()
    do {
        run.invoke()?.also {
            //if 耗时操作
            return it
        }
        if (interval > 0) delay(interval)
        else ensureActive()
    } while (SystemClock.elapsedRealtime() - begin < waitMillis)
    return null
}


internal suspend inline fun ensureActive() {
    coroutineContext.ensureActive()
}


fun jumpAccessibilityServiceSettings(cls: Class<*>) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.putComponent(InitCp.AppIns.packageName, cls)
    InitCp.AppIns.startActivity(intent)
}

private fun Intent.putComponent(pkg: String, cls: Class<*>) {
    val cs = ComponentName(pkg, cls.name).flattenToString()
    val bundle = Bundle()
    bundle.putString(":settings:fragment_args_key", cs)
    putExtra(":settings:fragment_args_key", cs)
    putExtra(":settings:show_fragment_args", bundle)
}

fun compareSimilarity(str1: String, str2: String, ignoreCase: Boolean = true): Float {
    var s1 = str1
    var s2 = str2
    if (ignoreCase) {
        s1 = str1.lowercase(Locale.getDefault())
        s2 = str2.lowercase(Locale.getDefault())
    }
    //计算两个字符串的长度
    val len1 = s1.length
    val len2 = s2.length
    //建立上面说的数组，比字符长度大一个空间
    val dif = Array(len1 + 1) { IntArray(len2 + 1) }
    //赋初值，步骤B
    for (a in 0..len1) {
        dif[a][0] = a
    }
    for (a in 0..len2) {
        dif[0][a] = a
    }
    //计算两个字符是否一样，计算左上的值
    var temp: Int
    for (i in 1..len1) {
        for (j in 1..len2) {
            temp = if (s1[i - 1] == s2[j - 1]) {
                0
            } else {
                1
            }
            //取三个值中最小的
            dif[i][j] = arrayOf(
                dif[i - 1][j - 1] + temp, dif[i][j - 1] + 1,
                dif[i - 1][j] + 1
            ).minOrNull()!!
        }
    }
    return 1 - dif[len1][len2].toFloat() / max(s1.length, s2.length)
}

operator fun String.times(number: Int): String {
    return buildString {
        for (i in 1..number) {
            append(this@times)
        }
    }
}
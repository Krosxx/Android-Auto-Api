package cn.vove7.andro_accessibility_api

import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {


    fun f(): Boolean {
        println("false")
        return false
    }

    fun t(): Boolean {
        println("true")
        return true
    }

    @Test
    fun addition_isCorrect() {
        val a = (t() || f() && t())
    }
}
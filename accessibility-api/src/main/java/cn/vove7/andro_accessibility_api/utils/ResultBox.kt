package cn.vove7.andro_accessibility_api.utils

import java.util.concurrent.CountDownLatch

/**
 * Created by Vove on 2018/7/5
 */
class ResultBox<T> {
    private var mValue: T? = null

    var lock = CountDownLatch(1)

    constructor()

    constructor(initValue: T) {
        this.mValue = initValue
    }

    fun setAndNotify(value: T) {
        mValue = value
        lock.countDown()
    }

    //等待结果
    @Throws(InterruptedException::class)
    fun blockedGet(safely: Boolean = true): T? {
        if (lock.count <= 0) return mValue
        if (safely) {
            kotlin.runCatching {
                lock.await()
            }
        } else {
            lock.await()
        }
        return mValue
    }


}

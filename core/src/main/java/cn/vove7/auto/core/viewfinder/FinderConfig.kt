package cn.vove7.auto.core.viewfinder

import cn.vove7.auto.core.BuildConfig

object FinderConfig {

    var DEBUG_LOG = BuildConfig.DEBUG

    // ViewFinder.require timeout millis, max 30.sec
    var FINDER_WAIT_MILLIS = 3000L
    var FINDER_WAIT_INTERVAL = 20L

    // Global default includeInvisible
    var FINDER_INCLUDE_INVISIBLE = false

    // @see ViewFinder.rootCompat
    var FINDER_ROOT_COMPAT = false

    // ViewNode.performAction number of attempts
    var TRY_OP_CNT = 10

    internal var ENABLE_FIND_FAILED_STRATEGY = false

    internal var onFindFailed: ((ViewFinder<*>) -> Unit)? = null

    fun enableFindFailedStrategy(s: (ViewFinder<*>) -> Unit) {
        ENABLE_FIND_FAILED_STRATEGY = true
        onFindFailed = s
    }

    fun init(configBlock: FinderConfig.() -> Unit) = apply(configBlock)
}
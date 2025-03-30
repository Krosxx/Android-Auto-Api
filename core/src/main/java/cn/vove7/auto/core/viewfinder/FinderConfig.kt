package cn.vove7.auto.core.viewfinder

object FinderConfig {

    // ViewFinder.require timeout millis
    var FINDER_WAIT_MILLIS = 3000L
    var FINDER_WAIT_INTERVAL = 20L

    // ViewNode.performAction number of attempts
    var TRY_OP_CNT = 10

    internal var ENABLE_FIND_FAILED_STRATEGY = false

    internal var onFindFailed: ((ViewFinder<*>) -> Unit)? = null

    fun enableFindFailedStrategy(s: (ViewFinder<*>) -> Unit) {
        ENABLE_FIND_FAILED_STRATEGY = true
        onFindFailed = s
    }

    fun init(configBlock: FinderConfig.() -> Unit) {
        configBlock(this)
    }

}
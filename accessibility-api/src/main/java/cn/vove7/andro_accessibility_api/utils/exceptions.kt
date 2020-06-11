package cn.vove7.andro_accessibility_api.utils

import cn.vove7.andro_accessibility_api.viewfinder.ViewFinder

/**
 * # exceptions
 * 异常类合集
 * @author Administrator
 * 2018/12/20
 */

/**
 * 视图搜索失败异常
 */
class ViewNodeNotFoundException(finder: ViewFinder?) : Exception(
    finder?.toString()
        ?: "no finder"
)

/**
 * 无障碍服务未运行异常
 * @constructor
 */
class NeedAccessibilityException(name: String?) : RuntimeException("无障碍服务未运行: $name")

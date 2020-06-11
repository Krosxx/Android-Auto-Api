package cn.vove7.andro_accessibility_api

/**
 * # AppScope
 *
 * @property packageName String  app pkg
 * @property pageName String  class name of Activity or Dialog
 * @constructor
 */
data class AppScope(
    var packageName: String,
    var pageName: String
) {
    override fun equals(that: Any?): Boolean {
        if (that == null || that !is AppScope) return false

        return packageName.startsWith(that.packageName) &&
                pageName.isEmpty() || that.pageName.isEmpty() ||
                pageName.endsWith("." + that.pageName) ||
                pageName.endsWith("$" + that.pageName) ||
                that.pageName.endsWith(".$pageName") ||
                that.pageName.endsWith("$$pageName")

    }
}
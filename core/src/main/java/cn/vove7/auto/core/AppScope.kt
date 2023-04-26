package cn.vove7.auto.core

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
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is AppScope) return false

        return packageName.startsWith(other.packageName) &&
                (pageName == other.pageName ||
                pageName.isEmpty() || other.pageName.isEmpty() ||
                pageName.endsWith("." + other.pageName) ||
                pageName.endsWith("$" + other.pageName) ||
                other.pageName.endsWith(".$pageName") ||
                other.pageName.endsWith("$$pageName"))

    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + pageName.hashCode()
        return result
    }
}
package cn.vove7.auto.core

/**
 * # AppPageInfo
 *
 * @property packageName String  app pkg
 * @property pageName String  class name of Activity or Dialog
 * @constructor
 */
@Deprecated(
    "replace with AppPageInfo",
    ReplaceWith("AppPageInfo", "cn.vove7.auto.core"),
)
class AppScope(packageName: String, pageName: String) : AppPageInfo(packageName, pageName)

open class AppPageInfo(
    val packageName: String,
    val pageName: String
) {
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is AppPageInfo) return false

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

    override fun toString(): String {
        return "AppPageInfo(packageName='$packageName', pageName='$pageName')"
    }

}
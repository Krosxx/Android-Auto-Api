package cn.vove7.andro_accessibility_api.viewfinder

import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import cn.vove7.andro_accessibility_api.utils.compareSimilarity
import cn.vove7.andro_accessibility_api.viewnode.ViewNode
import kotlin.coroutines.CoroutineContext

/**
 * # ViewFinderWithMultiCondition
 * 多条件查询
 * @author 17719
 * 2018/8/5
 */
@Deprecated(
    "use SmartFinder",
    ReplaceWith("SF", "cn.vove7.andro_accessibility_api.viewfinder.*")
)
open class ViewFinderWithMultiCondition(
    override val node: ViewNode? = null
) : ViewFinder<ViewFinderWithMultiCondition> {
    override var coroutineCtx: CoroutineContext? = null

    private var viewTextCondition: MutableList<String> = mutableListOf()

    fun addViewTextCondition(vararg s: String) {
        viewTextCondition.addAll(s)
    }

    var textMatchMode: Int = 0
    var descMatchMode: Int = 0
    var viewId: String? = null
    var descTexts: MutableList<String> = mutableListOf()
    var editable: Boolean? = null
    var scrollable: Boolean? = null
    var typeNames: MutableList<String> = mutableListOf()
    var textLengthLimit: IntRange? = null

    /**
     * 按查找条件查询
     * id text desc type
     * scrollable editable
     * @param node AccessibilityNodeInfo
     * @return Boolean
     */
    override fun findCondition(node: AccessibilityNodeInfo): Boolean {
        //could not remove "$.." prevent cause null
        if (viewId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            val vid = node.viewIdResourceName ?: ""
            if (!vid.endsWith("/$viewId") && vid != viewId)
            // :id/view_id) //网页视图 id : id
                return false
        }

        val viewText = node.text
        //文字长度
        val tl = textLengthLimit
        if (tl != null && viewText != null && !tl.contains(viewText.length)) {
            return false
        }
        //匹配文字
        if (!matchTextWithCondition(textMatchMode, node.text?.toString(), viewTextCondition)) {
            return false
        }
        //匹配说明
        if (!matchTextWithCondition(
                descMatchMode,
                node.contentDescription?.toString(),
                descTexts
            )
        ) {
            return false
        }
        //匹配className
        if (typeNames.isNotEmpty()) {
            var ok = false
            for (it in typeNames) {
                val v = "${node.className}".contains(it, ignoreCase = true)
                if (v) {
                    ok = true
                    break
                }
            }
            if (!ok) return false
        }
        //可滑动
        if (scrollable != null && node.isScrollable != scrollable) {
            return false
        }
        //可编辑
        if (editable != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
            && node.isEditable != editable
        ) {
            return false
        }
        return true
    }

    override fun toString(): String {
        return "ViewFinder(" +
                (if (viewTextCondition.isNotEmpty()) "viewTextCondition=$viewTextCondition" else "") +
                ", textMatchMode=$textMatchMode" +
                (if (viewId != null) ", viewId=$viewId" else "") +
                (if (descTexts.isNotEmpty()) ", desc=$descTexts" else "") +
                (if (typeNames.isNotEmpty()) ", typeNames=$typeNames" else "") +
                (if (editable == true) ", editable=$editable" else "") +
                (if (scrollable == true) ", scrollable=$scrollable)" else "") + ")"
    }

    companion object {
        const val TEXT_MATCH_MODE_EQUAL = 1
        const val TEXT_MATCH_MODE_REGEX = 2
        const val TEXT_MATCH_MODE_CONTAIN = 3
        const val TEXT_MATCH_MODE_FUZZY_WITHOUT_PINYIN = 4
        //        const val TEXT_MATCH_MODE_FUZZY_WITH_PINYIN = 5

        var TEXT_SIMILARITY = 0.75

        private val matchFunctions = mutableMapOf(
            TEXT_MATCH_MODE_EQUAL to { it: String, text: String ->
                it.equals(text, ignoreCase = true)
            },
            TEXT_MATCH_MODE_CONTAIN to { it: String, text: String ->
                text.contains(it, ignoreCase = true)
            },
            TEXT_MATCH_MODE_REGEX to { it: String, text: String ->
                it.toRegex().matches(text)
            },
            TEXT_MATCH_MODE_FUZZY_WITHOUT_PINYIN to { it: String, text: String ->
                compareSimilarity(text, it) >= TEXT_SIMILARITY
            }
        )

        /**
         * 扩展文本匹配方法
         *
         * @param type Int
         * @param onMatch Function2<[@kotlin.ParameterName] String, [@kotlin.ParameterName] String?, Boolean>
         */
        fun addMatchFunction(type: Int, onMatch: (it: String, text: String?) -> Boolean) {
            matchFunctions[type] = onMatch
        }

        /**
         * 根据规则匹配文本
         * @param type Int
         * @param text String?
         * @param ms List<String>
         * @return Boolean true 继续  false 匹配失败
         */
        private fun matchTextWithCondition(type: Int, text: String?, ms: List<String>): Boolean {
            if (ms.isEmpty()) return true
            if (text == null && ms.isNotEmpty()) return false
            val matchFunc = matchFunctions[type] ?: return false
            return ms.any { matchFunc(it, text ?: "") }
        }

    }
}

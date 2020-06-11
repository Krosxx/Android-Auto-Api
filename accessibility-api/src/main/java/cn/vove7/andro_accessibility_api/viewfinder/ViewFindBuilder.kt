@file:Suppress("unused")

package cn.vove7.andro_accessibility_api.viewfinder

import android.util.Range
import cn.vove7.andro_accessibility_api.viewfinder.ViewFinderWithMultiCondition.Companion.TEXT_MATCH_MODE_CONTAIN
import cn.vove7.andro_accessibility_api.viewfinder.ViewFinderWithMultiCondition.Companion.TEXT_MATCH_MODE_EQUAL
import cn.vove7.andro_accessibility_api.viewfinder.ViewFinderWithMultiCondition.Companion.TEXT_MATCH_MODE_FUZZY_WITHOUT_PINYIN
import cn.vove7.andro_accessibility_api.viewfinder.ViewFinderWithMultiCondition.Companion.TEXT_MATCH_MODE_REGEX
import cn.vove7.andro_accessibility_api.viewnode.ViewNode


/**
 * @author Vove
 *
 * 视图节点查找类
 *
 * 2018/8/5
 */
class ViewFindBuilder : FindBuilderWithOperation {

    internal val viewFinderX: ViewFinderWithMultiCondition
        get() = finder as ViewFinderWithMultiCondition

    /**
     * DSL
     * @param builder [@kotlin.ExtensionFunctionType] Function1<ViewFindBuilder, Unit>
     */
    operator fun invoke(builder: ViewFindBuilder.() -> Unit) {
        apply(builder)
    }

    constructor() {
        finder = ViewFinderWithMultiCondition()
    }

    constructor(startNode: ViewNode) {
        finder = ViewFinderWithMultiCondition(startNode)
    }


    fun depths(ds: Array<Int>): ViewFindBuilder {
        viewFinderX.depths = ds
        return this
    }

    /**
     * 包含文本
     *
     * @param text text
     * @return this
     */
    fun containsText(vararg text: String): ViewFindBuilder {
        viewFinderX.addViewTextCondition(*text)
        viewFinderX.textMatchMode = TEXT_MATCH_MODE_CONTAIN
        return this
    }

    fun containsText(text: String): ViewFindBuilder {
        viewFinderX.addViewTextCondition(text)
        viewFinderX.textMatchMode = TEXT_MATCH_MODE_CONTAIN
        return this
    }

    /**
     * 正则匹配
     *
     * @param regs 表达式 %消息%
     * @return this
     */
    fun matchesText(vararg regs: String): ViewFindBuilder {
        viewFinderX.addViewTextCondition(*regs)
        viewFinderX.textMatchMode = TEXT_MATCH_MODE_REGEX
        return this
    }

    fun matchesText(regs: String): ViewFindBuilder {
        viewFinderX.addViewTextCondition(regs)
        viewFinderX.textMatchMode = TEXT_MATCH_MODE_REGEX
        return this
    }


    /**
     * 相同文本 不区分大小写
     *
     * @param text text
     * @return this
     */
    fun text(vararg text: String): ViewFindBuilder = equalsText(*text)

    fun equalsText(vararg text: String): ViewFindBuilder {
        viewFinderX.addViewTextCondition(*text)
        viewFinderX.textMatchMode = TEXT_MATCH_MODE_EQUAL
        return this
    }

    fun text(text: String): ViewFindBuilder = equalsText(text)

    fun equalsText(text: String): ViewFindBuilder {
        viewFinderX.addViewTextCondition(text)
        viewFinderX.textMatchMode = TEXT_MATCH_MODE_EQUAL
        return this
    }

    /**
     * 文本相似度
     *
     * @param text 文本内容
     * @return this
     */
    fun similaryText(vararg text: String): ViewFindBuilder {
        viewFinderX.addViewTextCondition(*text)
        viewFinderX.textMatchMode = TEXT_MATCH_MODE_FUZZY_WITHOUT_PINYIN
        return this
    }


    fun textLengthLimit(lower: Int = 0, upper: Int): ViewFindBuilder {
        viewFinderX.textLengthLimit = Range.create(lower, upper)
        return this
    }


    /**
     * 根据id 查找
     *
     * @param id viewId
     * @return
     */
    fun id(id: String): ViewFindBuilder {
        viewFinderX.viewId = id
        return this
    }

    /**
     * 说明
     *
     * @param desc
     * @return
     */
    fun desc(vararg desc: String): ViewFindBuilder {
        viewFinderX.descTexts.addAll(listOf(*desc))
        viewFinderX.descMatchMode = TEXT_MATCH_MODE_EQUAL
        return this
    }

    fun containsDesc(vararg desc: String): ViewFindBuilder {
        viewFinderX.descTexts.addAll(listOf(*desc))
        viewFinderX.descMatchMode = TEXT_MATCH_MODE_CONTAIN
        return this
    }

    @JvmOverloads
    fun editable(b: Boolean = true): ViewFindBuilder {
        viewFinderX.editable = b
        return this
    }

    @JvmOverloads
    fun scrollable(b: Boolean = true): ViewFindBuilder {
        viewFinderX.scrollable = b
        return this
    }

    fun type(vararg types: String): ViewFindBuilder {
        viewFinderX.typeNames.addAll(listOf(*types))
        return this
    }

    fun await(): ViewNode? {
        return waitFor()
    }

    fun await(l: Long): ViewNode? {
        return waitFor(l)
    }

}

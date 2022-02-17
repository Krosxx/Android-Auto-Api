@file:Suppress("unused")

package cn.vove7.andro_accessibility_api.viewfinder

import cn.vove7.andro_accessibility_api.viewnode.ViewNode


/**
 * @author Vove
 *
 * Builder of ViewFinderWithMultiCondition
 *
 * 2018/8/5
 */
class ViewFindBuilder(
    node: ViewNode? = null
) : ViewFinderWithMultiCondition(node),
    FinderBuilderWithOperation {

    /**
     * DSL
     * @param builder [@kotlin.ExtensionFunctionType] Function1<ViewFindBuilder, Unit>
     */
    operator fun invoke(builder: ViewFindBuilder.() -> Unit) {
        apply(builder)
    }

    override var finder: ViewFinder<*> = this

    /**
     * 包含文本
     *
     * @param text text
     * @return this
     */
    fun containsText(vararg text: String): ViewFindBuilder {
        addViewTextCondition(*text)
        textMatchMode = TEXT_MATCH_MODE_CONTAIN
        return this
    }

    fun containsText(text: String): ViewFindBuilder {
        addViewTextCondition(text)
        textMatchMode = TEXT_MATCH_MODE_CONTAIN
        return this
    }

    /**
     * 正则匹配
     *
     * @param regs 表达式 %消息%
     * @return this
     */
    fun matchesText(vararg regs: String): ViewFindBuilder {
        addViewTextCondition(*regs)
        textMatchMode = TEXT_MATCH_MODE_REGEX
        return this
    }

    fun matchesText(regs: String): ViewFindBuilder {
        addViewTextCondition(regs)
        textMatchMode = TEXT_MATCH_MODE_REGEX
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
        addViewTextCondition(*text)
        textMatchMode = TEXT_MATCH_MODE_EQUAL
        return this
    }

    fun text(text: String): ViewFindBuilder = equalsText(text)

    fun equalsText(text: String): ViewFindBuilder {
        addViewTextCondition(text)
        textMatchMode = TEXT_MATCH_MODE_EQUAL
        return this
    }

    /**
     * 文本相似度
     *
     * @param text 文本内容
     * @return this
     */
    fun similaryText(vararg text: String): ViewFindBuilder {
        addViewTextCondition(*text)
        textMatchMode = TEXT_MATCH_MODE_FUZZY_WITHOUT_PINYIN
        return this
    }

    fun textLengthLimit(lower: Int = 0, upper: Int): ViewFindBuilder {
        textLengthLimit = lower until upper
        return this
    }

    /**
     * 根据id 查找
     *
     * @param id viewId
     * @return
     */
    fun id(id: String): ViewFindBuilder {
        viewId = id
        return this
    }

    /**
     * 说明
     *
     * @param desc
     * @return
     */
    fun desc(vararg desc: String): ViewFindBuilder {
        descTexts.addAll(listOf(*desc))
        descMatchMode = TEXT_MATCH_MODE_EQUAL
        return this
    }

    fun containsDesc(vararg desc: String): ViewFindBuilder {
        descTexts.addAll(listOf(*desc))
        descMatchMode = TEXT_MATCH_MODE_CONTAIN
        return this
    }

    @JvmOverloads
    fun editable(b: Boolean = true): ViewFindBuilder {
        editable = b
        return this
    }

    @JvmOverloads
    fun scrollable(b: Boolean = true): ViewFindBuilder {
        scrollable = b
        return this
    }

    fun type(vararg types: String): ViewFindBuilder {
        typeNames.addAll(listOf(*types))
        return this
    }

}

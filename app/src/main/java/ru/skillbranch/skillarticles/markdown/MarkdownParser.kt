package ru.skillbranch.skillarticles.markdown

import java.util.regex.Pattern

object MarkdownParser {

    private val LINE_SEPARATOR = System.getProperty("line.separator") ?: "\n"
    private const val LINK_JUST_TITLE = "((?<!\\[)\\[.+\\](?!\\]))"
    private const val LINK_JUST_LINK = "((?<!\\()\\(.+\\)(?!\\)))"
    private const val IMAGE_JUST_ALT = "(!(?<!\\[)\\[.*](?!]))"
    private const val IMAGE_URL_WITH_TITLE = "((?<!\\()\\(.*\\)(?!\\)))"
    private const val IMAGE_JUST_TITLE = "(\".*\")"

    //group regex
    private const val UNORDERED_LIST_ITEM_GROUP = "(^[*+-] .+$)"
    private const val HEADER_GROUP = "(^#{1,6} .+$)"
    private const val QUOTE_GROUP = "(^> .+$)"
    private const val ITALIC_GROUP = "((?<!\\*)\\*[^*].*?[^*]?\\*(?!\\*)|(?<!_)_[^_].*?[^_]?_(?!_))"
    private const val BOLD_GROUP =
        "((?<!\\*)\\*{2}[^*].*?[^*]?\\*{2}(?!\\*)|(?<!_)_{2}[^_].*?[^_]?_{2}(?!_))"
    private const val STRIKE_GROUP = "((?<!~)~{2}[^~].*?[^~]?~{2}(?!~))"
    private const val RULE_GROUP = "(^[-_*]{3}$)"
    private const val INLINE_GROUP = "((?<!`)`[^`\\s].*?[^`\\s]?`(?!`))"
    private const val MULTILINE_GROUP = "((?<!`)`{3}[^`\\s][.\\w\\s\\d]*[^`\\s]`{3}(?!`))"
    private const val ORDERED_LIST_ITEM_GROUP = "(^\\d+\\. .+\$)"
    private const val LINK_GROUP = "$LINK_JUST_TITLE$LINK_JUST_LINK"
    private const val IMAGE_GROUP = "(!(?<!\\[)\\[.*](?!])(?<!\\()\\(.*\\)(?!\\)))"


    //result regex
    private const val MARKDOWN_GROUPS = "$UNORDERED_LIST_ITEM_GROUP|$HEADER_GROUP|$QUOTE_GROUP|" +
            "$ITALIC_GROUP|$BOLD_GROUP|$STRIKE_GROUP|$RULE_GROUP|$INLINE_GROUP|$MULTILINE_GROUP|" +
            "$ORDERED_LIST_ITEM_GROUP|$IMAGE_GROUP|$LINK_GROUP"

    private val elementsPattern by lazy { Pattern.compile(MARKDOWN_GROUPS, Pattern.MULTILINE) }

    /**
     * parse markdown text to elements
     */
    fun parse(string: String): MarkdownText {
        val elements = mutableListOf<Element>()
        elements.addAll(findElements(string))
        return MarkdownText(elements)
    }

    /**
     * clear markdown text to string without markdown characters
     */
    fun clear(string: String): String? {
        val elements = mutableListOf<Element>()
        elements.addAll(findElements(string))

        return if (elements.isEmpty()) null
        else {
            elements.fold("") { acc: String, element: Element ->
                acc.plus(element.spreadText())
            }
        }
    }

    // проходится по списку распарсенных элементов и возвращает все тексты максимальной вложенности
    // (максимально очищенные)
    private fun Element.spreadText(): String {
        return if (this.elements.isEmpty()) {
            this.text.toString()
        } else {
            var resultText = ""
            this.elements.forEach { element -> resultText += element.spreadText() }
            resultText
        }
    }

    //парсит строку и возвращает лист элементов
    private fun findElements(string: CharSequence): List<Element> {
        val parents = mutableListOf<Element>()
        val matcher = elementsPattern.matcher(string)
        var lastStartIndex = 0

        loop@ while (matcher.find(lastStartIndex)) {
            val startIndex = matcher.start()
            val endIndex = matcher.end()

            // if something is found then everything before - TEXT
            if (lastStartIndex < startIndex) {
                parents.add(Element.Text(string.subSequence(lastStartIndex, startIndex)))
            }

            //found text
            val text: CharSequence

            //group range for iterate by groups
            val groups: IntRange = 1..12
            var group = -1


            for (gr in groups) {
                if (matcher.group(gr) != null) {
                    group = gr
                    break
                }
            }

            when (group) {
                //NOT FOUND -> BREAK
                -1 -> break@loop
                //UNORDERED LIST
                1 -> {
                    //text without "*. "
                    text = string.subSequence(startIndex.plus(2), endIndex)

                    //find inner elements
                    val subs = findElements(text)
                    val element = Element.UnorderedListItem(text, subs)
                    parents.add(element)

                    //next find start from position "endIndex" (last regex character)
                    lastStartIndex = endIndex
                }
                //HEADER
                2 -> {
                    val reg = "^#{1,6}".toRegex().find(string.subSequence(startIndex, endIndex))
                    val level = reg!!.value.length

                    // text without "{#} "
                    text = string.subSequence(startIndex.plus(level.inc()), endIndex)

                    val element = Element.Header(level, text)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                //QUOTE
                3 -> {
                    // text without "> "
                    text = string.subSequence(startIndex.plus(2), endIndex)
                    val subElements = findElements(text)
                    val element = Element.Quote(text, subElements)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                //ITALIC
                4 -> {
                    // text without "*{}*" or "_{}_"
                    text = string.subSequence(startIndex.inc(), endIndex.dec())
                    val subElements = findElements(text)
                    val element = Element.Italic(text, subElements)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                //BOLD
                5 -> {
                    // text without "**{}**" or "__{}__"
                    text = string.subSequence(startIndex.plus(2), endIndex.plus(-2))
                    val subElements = findElements(text)
                    val element = Element.Bold(text, subElements)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                //STRIKE
                6 -> {
                    // text without "~~{}~~"
                    text = string.subSequence(startIndex.plus(2), endIndex.plus(-2))
                    val subElements = findElements(text)
                    val element = Element.Strike(text, subElements)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                //RULE
                7 -> {
                    // text without "***" or "---" or "___" insert empty character
                    val element = Element.Rule()
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                //INLINE CODE
                8 -> {
                    // text without "`{}`"
                    text = string.subSequence(startIndex.inc(), endIndex.dec())
                    val subElements = findElements(text)
                    val element = Element.InlineCode(text, subElements)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                //MULTILINE CODE
                9 -> {
                    // text without "```{}```"
                    text = string.subSequence(startIndex.plus(3), endIndex.plus(-3))
                    val subElements = findElements(text)
                    val element = Element.BlockCode(text, subElements)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                //ORDERED LIST ITEM
                10 -> {
                    val reg = "^\\d+\\.".toRegex().find(string.subSequence(startIndex, endIndex))
                    val order = reg!!.value.length
                    // text without "1. {}"
                    text = string.subSequence(startIndex.plus(order + 1), endIndex)
                    val subElements = findElements(text)
                    val element = Element.OrderedListItem(order = reg.value, text, subElements)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                //IMAGE
                11 -> {
                    val regAlt = IMAGE_JUST_ALT.toRegex()
                        .find(string.subSequence(startIndex, endIndex))
                    val regUrlWithTitle = IMAGE_URL_WITH_TITLE.toRegex()
                        .find(string.subSequence(startIndex, endIndex))
                    val regTitle = IMAGE_JUST_TITLE.toRegex()
                        .find(string.subSequence(startIndex, endIndex))
                    // alt -> text without "![{}]"
                    val alt = regAlt?.value?.subSequence(2, regAlt.value.length.plus(-1))
                    // url with title -> text without "({})"
                    val urlWithTitle = regUrlWithTitle!!.value.subSequence(
                        1,
                        regUrlWithTitle.value.length.plus(-1)
                    )
                    val title =
                        regTitle?.value?.subSequence(1, regTitle.value.length.plus(-1)) ?: ""
                    text = title

                    val element = if (text.isBlank()) {
                        Element.Image(
                            url = urlWithTitle.toString(),
                            if (alt.isNullOrBlank()) null else alt.toString(),
                            text,
                            emptyList()
                        )
                    } else {
                        val subElements = findElements(text)
                        Element.Image(
                            url = urlWithTitle.subSequence(
                                0,
                                urlWithTitle.length.plus(-text.length).plus(-3)
                            ).toString(),
                            if (alt.isNullOrBlank()) null else alt.toString(),
                            text,
                            subElements
                        )
                    }
                    parents.add(element)
                    lastStartIndex = endIndex
                }
                //LINK
                12 -> {
                    val regTitle = LINK_JUST_TITLE.toRegex()
                        .find(string.subSequence(startIndex, endIndex))
                    val regLink = LINK_JUST_LINK.toRegex()
                        .find(string.subSequence(startIndex, endIndex))
                    // title -> text without "[{}]"
                    text = regTitle!!.value.subSequence(1, regTitle.value.length.plus(-1))
                    // link -> text without "({})"
                    val link = regLink!!.value.subSequence(1, regLink.value.length.plus(-1))
                    val subElements = findElements(text)
                    val element = Element.Link(link = link.toString(), text, subElements)
                    parents.add(element)
                    lastStartIndex = endIndex
                }
            }
        }

        if (lastStartIndex < string.length) {
            parents.add(Element.Text(string.subSequence(lastStartIndex, string.length)))
        }

        return parents
    }
}

data class MarkdownText(val elements: List<Element>)

sealed class Element {
    abstract val text: CharSequence
    abstract val elements: List<Element>

    data class Text(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class UnorderedListItem(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Header(
        val level: Int = 1,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Quote(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Italic(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Bold(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Strike(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    // FIXME: 12.09.2021 учитывать default text for clear
    data class Rule(
        override val text: CharSequence = " ", //for insert span
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class InlineCode(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Link(
        val link: String,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class OrderedListItem(
        val order: String,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class BlockCode(
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()

    data class Image(
        val url: String,
        val alt: String?,
        override val text: CharSequence,
        override val elements: List<Element> = emptyList()
    ) : Element()
}
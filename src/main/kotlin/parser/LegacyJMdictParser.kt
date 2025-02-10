package parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.File

interface JMdictParser {

    fun parse(file: File): List<JMdictItem>

    companion object {
        val Instance: JMdictParser = DefaultJMdictParser
    }

}

data class JMdictItem(
    val entrySequence: String,
    val elements: List<JMDictElement>,
    val glossaryItems: List<JMDictGlossaryItem>
)

data class JMDictElement(
    val expression: String,
    val type: JMDictElementType,
    val priorities: List<JMDictPriority>
) {

    val priority: Int? = priorities.minOfOrNull { it.asNumber() }

}

enum class JMDictElementType { Kanji, Reading }

interface JMDictPriority {

    val range: IntRange
    fun asNumber(): Int = (range.first + range.last) / 2

    companion object {
        fun fromJMDictValue(value: String): JMDictPriority {
            val number = value.takeLastWhile { it.isDigit() }.toInt()
            return when {
                value.startsWith("news") -> News(number)
                value.startsWith("ichi") -> Ichi(number)
                value.startsWith("spec") -> Spec(number)
                value.startsWith("gai") -> Gai(number)
                value.startsWith("nf") -> NF(number)
                else -> throw IllegalStateException("unknown priority $value")
            }
        }
    }

    class News(private val number: Int) : JMDictPriority {
        override val range = if (number == 1) 1 until 11573 else 11573 until 22160
    }

    class Ichi(private val number: Int) : JMDictPriority {
        override val range = if (number == 1) 1 until 8508 else 8508 until 17039
    }

    class Spec(private val number: Int) : JMDictPriority {
        override val range = if (number == 1) 1 until 1317 else 1317 until 2929
    }

    class Gai(private val number: Int) : JMDictPriority {
        override val range = if (number == 1) 1 until 20000 else 20000 until 40000
    }

    class NF(private val number: Int) : JMDictPriority {
        override val range = (number - 1) * 500 until number * 500
    }
}

data class JMDictGlossaryItem(
    val language: String,
    val text: String
)

private object DefaultJMdictParser : JMdictParser {

    override fun parse(file: File): List<JMdictItem> {
        return Jsoup.parse(file, Charsets.UTF_8.name())
            .select("entry")
            .map { dicEntry ->

                val elements = dicEntry.children().mapNotNull {
                    when (it.tagName()) {
                        "k_ele" -> JMDictElement(
                            expression = it.selectFirst("keb")?.text()!!,
                            type = JMDictElementType.Kanji,
                            priorities = it.select("ke_pri").map { it.toPriority() }
                        )

                        "r_ele" -> JMDictElement(
                            expression = it.selectFirst("reb")?.text()!!,
                            type = JMDictElementType.Reading,
                            priorities = it.select("re_pri").map { it.toPriority() }
                        )

                        else -> null
                    }
                }

                JMdictItem(
                    entrySequence = dicEntry.selectFirst("ent_seq")!!.text(),
                    elements = elements,
                    glossaryItems = dicEntry.select("gloss").map {
                        JMDictGlossaryItem(
                            language = it.attr("xml:lang").takeIf { it.isNotEmpty() } ?: "en",
                            text = it.text()
                        )
                    }
                )
            }
    }

    private fun Element.toPriority(): JMDictPriority = JMDictPriority.fromJMDictValue(text())

}
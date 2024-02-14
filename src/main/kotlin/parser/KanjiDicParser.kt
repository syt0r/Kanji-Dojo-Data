package parser

import org.jsoup.Jsoup
import java.io.File

interface KanjiDicParser {

    companion object {
        val Instance: KanjiDicParser = DefaultKanjiDicParser
    }

    fun parse(file: File): List<KanjiDicEntry>

}

data class KanjiDicEntry(
    val character: String,
    val meanings: List<KanjiDicMeaning>,
    val onReadings: List<String>,
    val kunReadings: List<String>,
    val grade: Int?,
    val frequency: Int?,
    val isRadical: Boolean,
    val strokeCount: Int,
    val alternativeEncodings: List<KanjiDicCharacterEncodingData>,
    val variants: List<KanjiDicCharacterEncodingData>
)

data class KanjiDicMeaning(
    val language: String?,
    val value: String
)

data class KanjiDicCharacterEncodingData(
    val encoding: String,
    val value: String
)

private object DefaultKanjiDicParser : KanjiDicParser {

    override fun parse(file: File): List<KanjiDicEntry> {
        return Jsoup.parse(file, Charsets.UTF_8.name()).select("character").map { element ->

            val readingElements = element.select("reading")

            val kunReadings = readingElements.toList()
                .filter { it.attr("r_type").startsWith("ja_kun") }
                .map { it.text() }

            val onReadings = readingElements.toList()
                .filter { it.attr("r_type").startsWith("ja_on") }
                .map { it.text() }

            val meanings = element.select("meaning")
                .map {
                    KanjiDicMeaning(
                        language = it.attr("m_lang").takeIf { it.isNotEmpty() },
                        value = it.text()
                    )
                }

            KanjiDicEntry(
                character = element.selectFirst("literal")!!.text(),
                meanings = meanings,
                kunReadings = kunReadings,
                onReadings = onReadings,
                grade = element.select("grade").text().toIntOrNull(),
                frequency = element.select("freq").text().toIntOrNull(),
                isRadical = element.select("radical").size > 0,
                strokeCount = element.selectFirst("stroke_count")!!.text().toInt(),
                alternativeEncodings = element.select("cp_value").map {
                    KanjiDicCharacterEncodingData(
                        encoding = it.attr("cp_type"),
                        value = it.text()
                    )
                },
                variants = element.select("variant").map {
                    KanjiDicCharacterEncodingData(
                        encoding = it.attr("var_type"),
                        value = it.text()
                    )
                }
            )
        }

    }

}
